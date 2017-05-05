/*
 *
 *  * Copyright IBM Corporation 2016-2017
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  * http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.ibm.csync.impl;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.gson.Gson;
import com.ibm.csync.CSync;
import com.ibm.csync.Deadline;
import com.ibm.csync.Key;
import com.ibm.csync.Listener;
import com.ibm.csync.Timeout;
import com.ibm.csync.Tracer;
import com.ibm.csync.Value;
import com.ibm.csync.impl.commands.Advance;
import com.ibm.csync.impl.commands.Data;
import com.ibm.csync.impl.commands.Fetch;
import com.ibm.csync.impl.commands.Sub;
import org.h2.jdbcx.JdbcDataSource;

import java.io.Closeable;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CSyncImpl {
	//private static Logger logger = LoggerFactory.getLogger(com.ibm.csync.impl.CSyncImpl.class);
	public static final Gson gson = new Gson();

	private final Database db;
	public final Transport ws;

	final Tracer tracer;

	final Set<Subscription> subscriptions = new HashSet<>();
	final Multiset<Key> activePatterns = ConcurrentHashMultiset.create();
	private final Executor listenExec = Executors.newSingleThreadExecutor(
		new ThreadFactoryBuilder()
			.setNameFormat("listen-%d")
			.setDaemon(true)
			.build());

	private final ScheduledExecutorService advanceThread = Executors.newScheduledThreadPool(1);
	public final ScheduledExecutorService workers;
	final CSync.Builder builder;

	private static final Timeout advanceTimeout = new Timeout(60000);

	public CSyncImpl(final CSync.Builder builder) throws Exception {
		//this.url = url;
		this.builder = builder;
		this.workers = builder.workers();
		this.tracer = builder.tracer();
		final JdbcDataSource ds = new JdbcDataSource();
		ds.setUrl("jdbc:h2:mem:test;DB_CLOSE_DELAY=-1");
		db = new Database(ds, subscriptions);

		// TODO: check uuid, reset database, etc.
		ws = new Transport(
			builder.url(),
			db,
			builder.workers(),
			tracer
		);
	}



	private void advanceException(final Key pattern, final Exception ex) {
		tracer.onError(ex,"advance for %s",pattern.string);

		// TODO: better scheduling
		advanceThread.schedule(
			() -> doAdvance(pattern),
			60,
			TimeUnit.SECONDS
		);
	}
	private void doAdvance(final Key pattern) {
		synchronized (subscriptions) {
			if (!activePatterns.contains(pattern)) return;
		}

		try {
			final Deadline deadline = Deadline.of(advanceTimeout);
			final long rvts = db.rvtsForPattern(pattern);
			final Advance.Response adv2res = Advance.send(this, pattern, rvts, deadline).get(deadline.ms, TimeUnit.MILLISECONDS);
			final List<Long> toFetch = db.shouldFetchVts(adv2res.vts);
			final Data.Response[] fetchResponse = Fetch.send(this, toFetch, deadline).get(deadline.ms, TimeUnit.MILLISECONDS);

			for (final Data.Response d : fetchResponse) {
				db.set(Value.of(d));
			}

			db.setRvts(pattern, adv2res.maxvts);

			// TODO: better scheduling policy
			workers.schedule(
				() -> doAdvance(pattern),
				10,
				TimeUnit.SECONDS
			);
		} catch (Exception ex) {
			advanceException(pattern,ex);
		}
	}


	///////////////
	// Listeners //
	///////////////


	// listen makes strong guarantees
	//    - in-order delivery for the same key
	//    - listener will eventually see the most recent update for all keys
	//
	// Data comes from different sources
	//    - live data from subscriptions
	//    - data from local database
	//    - data from advance
	//
	// Making the guarantees above without violating the guarantees above requires careful coordination
	//
	// Here is the plan:
	//    - All incoming data will be serialized through the database. Updates to a given key will be thrown away
	//      if they have a smaller VTS than the latest database entry for that key. This covers live data and advance.
	//
	//    - Local data is delivered directly to the listener and could violate those guarantees. We keep a
	//      map (key -> latestVTS) per subscription and use it to filter callbacks to the listener. This map
	//      is removed once we're done reading from the local store
	//
	//    - Listener callbacks for a given subscription are scheduled on a single thread (using a SingleThreadExecutor)

	public Closeable listen(final Key pattern, final Deadline dl, final Listener cb)  {
		final Subscription subscription = new Subscription(this, pattern,cb,listenExec);
		final boolean needToSchedule;

		// See if we need to create a new subscription or share an existing subscription
		synchronized (subscriptions) {
			// Add the subscription to the set of all subscriptions
			subscriptions.add(subscription);

			// If this pattern is not active, we need to send a Sub request to the server
			needToSchedule = !activePatterns.contains(pattern);

			// Add the pattern to the bag (keeps a count)
			activePatterns.add(pattern, 1);
		}

		if (needToSchedule) {
			// Send the Sub request. No need to wait for a reply except for error reporting
			Sub.send(this, pattern, dl)
				.exceptionally(e -> {tracer.onError(e,"sub %s",pattern.string); return null;});


			// TODO: listen / close / listen => 2 advances
			// Schedule an advance
			advanceThread.submit(() -> doAdvance(pattern));
		}

		workers.execute(() -> {
			try {
				db.getLocal(subscription);
			} catch (SQLException ex) {
				tracer.onError(ex,"getLocal %s",pattern.string);
			}
			// It is safe to delete the per-subscription filter now but it needs to be done on the subscription thread
			subscription.localLoadIsDone();
		});

		return subscription;
	}

}

