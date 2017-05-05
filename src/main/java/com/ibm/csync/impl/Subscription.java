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

import com.ibm.csync.CSync;
import com.ibm.csync.Deadline;
import com.ibm.csync.Listener;
import com.ibm.csync.Timeout;
import com.ibm.csync.Value;
import com.ibm.csync.Key;
import com.ibm.csync.impl.commands.Unsub;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

public class Subscription implements Closeable {

	//final private static Logger logger = LoggerFactory.getLogger(Subscription.class);

	private final CSyncImpl cSync;
	private final Key pattern;
	private final Executor callbackExec;
	private final Listener listener;

	// live data is serialized through the database, we keep this map until
	// we've finished loading the initial data from the database then remove once we're done
	private Map<Key,Long> latestVtsForKey = new HashMap<>();

	Subscription(CSyncImpl cSync, final Key pattern, final Listener listener, final Executor callbackExec) {
		this.cSync = cSync;
		this.pattern = pattern;
		this.callbackExec = callbackExec;
		this.listener = listener;
	}

	void call(final Value value) {
		if (pattern.matches(value.key)) {
			callbackExec.execute(() -> {
					if (latestVtsForKey != null) {
						Long it = latestVtsForKey.get(value.key);
						if (it != null) {
							if (value.vts <= it.longValue()) return;
						}
						latestVtsForKey.put(value.key, value.vts);
					}

					try {
						listener.call(value);
					} catch (Exception ex) {
						cSync.tracer.onError(ex,"listen");
					}

			} );
		}
	}

	void localLoadIsDone() {
		callbackExec.execute(() -> latestVtsForKey = null);
	}

	@Override
	public void close() {
		synchronized (cSync.subscriptions) {
			cSync.subscriptions.remove(this);
			final int nBeforeRemove = cSync.activePatterns.remove(pattern, 1);
			if (nBeforeRemove <= 1) {
				Unsub.send(cSync,pattern, Deadline.of(Timeout.of(10000)))
					.whenComplete((v,e) -> System.out.printf("*************** unsub e:%s v:%s\n",e,v)); // TODO:
			}
			// TODO: send unsub
		}
	}
}
