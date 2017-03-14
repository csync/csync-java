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

import com.ibm.csync.Value;
import com.ibm.csync.Key;
import org.h2.api.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

class Database {

	private static Logger logger = LoggerFactory.getLogger(Database.class);
	private final DataSource ds;
	private final Set<Subscription> subscriptions;
	//private final Executor exec;

	private final Map<Key, Long> rvtsCache = new HashMap<>();

	Database(final DataSource ds, final Set<Subscription> subscriptions) throws SQLException {
		this.ds = ds;
		this.subscriptions = subscriptions;
		//this.exec = exec;

		try (final Connection c = ds.getConnection()) {
			c.setAutoCommit(true);
			try (final Statement s = c.createStatement()) {
				s.execute("create table if not exists rvts (pattern varchar primary key)");
				s.execute("alter table rvts add if not exists rvts bigint not null default 0");
				s.execute("create table if not exists latest (" +
					"    key varchar primary key," +
					"    vts bigint not null unique," +
					"    deletePath boolean not null," +
					"    acl varchar not null," +
					"    creator varchar not null," +
					"    cts bigint not null," +
					"    value varchar" +
					")"
				);
			}
		}
	}

	private ResultSet query(final Connection c, final String sql, Object... args) throws SQLException {

		if (logger.isDebugEnabled()) {
			logger.debug("query {}", sql);
			for (Object arg : args) {
				logger.debug("    {}\n", arg);
			}
		}

		final PreparedStatement ps = c.prepareStatement(sql);
		for (int i = 0; i < args.length; i++) {
			ps.setObject(i + 1, args[i]);
		}
		return ps.executeQuery();
	}

	private int update(final Connection c, final String sql, Object... args) throws SQLException {

		if (logger.isDebugEnabled()) {
			logger.debug("update {}", sql);
			for (Object arg : args) {
				logger.debug("    {}", arg);
			}
		}

		final PreparedStatement ps = c.prepareStatement(sql);
		for (int i = 0; i < args.length; i++) {
			ps.setObject(i + 1, args[i]);
		}
		return ps.executeUpdate();

	}

	private Long getCachedRvts(final Key pattern) {
		synchronized (rvtsCache) {
			return rvtsCache.get(pattern);
		}
	}

	private void setCachedRvts(final Key pattern, final Long rvts) {
		synchronized (rvtsCache) {
			final Long cached = rvtsCache.get(pattern);
			if ((cached == null) || (rvts > cached)) {
				rvtsCache.put(pattern, rvts);
			}
		}
	}

	Long rvtsForPattern(final Key pattern) throws SQLException {
		final Long cached = getCachedRvts(pattern);
		if (cached != null) {
			return cached;
		}

		try (final Connection c = ds.getConnection()) {
			try (ResultSet rs = query(
				c,
				"SELECT rvts FROM rvts WHERE pattern = ?",
				pattern.string
			)) {
				final Long it = rs.next() ? rs.getLong(1) : 0L;
				setCachedRvts(pattern, it);
				return it;
			}
		}
	}

	int setRvts(final Key pattern, final Long rvts) throws SQLException {
		int cnt;

		try (final Connection c = ds.getConnection()) {
			c.setAutoCommit(true);

			// Try to insert the record
			try {
				cnt = update(c,
					"insert into rvts set pattern = ?, rvts = ?",
					pattern.string, rvts);
			} catch (SQLException ex) {
				if (ex.getErrorCode() != ErrorCode.DUPLICATE_KEY_1) throw ex;
				cnt = 0;
			}

			if (cnt == 0) {
				// Someone raced ahead of us, try t update it
				cnt = update(c,
					"update rvts set rvts = ?  where pattern = ? and rvts < ?",
					rvts, pattern.string, rvts);
			}
		}

		setCachedRvts(pattern, rvts);
		return cnt;
	}

	private boolean shouldFetchVts(final Long vts) throws SQLException {
		try (final Connection c = ds.getConnection()) {
			try (ResultSet rs = query(
				c,
				"SELECT key FROM latest WHERE vts = ?",
				vts
			)) {
				return !rs.next();
			}
		}
	}

	List<Long> shouldFetchVts(final Long[] vts) throws SQLException {
		List<Long> out = new ArrayList<>();
		for (Long v : vts) {
			if (shouldFetchVts(v)) {
				out.add(v);
			}
		}
		return out;
	}

	// TODO: explain this
	// Make it thread-safe
	private Map<Key, Object> activeUpdates = new WeakHashMap<>();

	boolean set(final Value value) throws SQLException {
		final Key key = value.key;
		final Object lock = activeUpdates.computeIfAbsent(key, k -> new Object());

		synchronized (lock) {

			try (final Connection c = ds.getConnection()) {
				c.setAutoCommit(true);

				int cnt;

				// Try to insert the record
				try {
					cnt = update(c,
						"insert into latest set key = ?, value = ?, vts = ?, cts = ?, deletePath = ?, acl = ?, creator = ?",
						key.string, value.data, value.vts, value.cts, value.isDeleted, value.acl.id(), value.creator);
				} catch (SQLException ex) {
					if (ex.getErrorCode() != ErrorCode.DUPLICATE_KEY_1) throw ex;
					cnt = 0;
				}

				if (cnt == 0) {
					// Someone raced ahead of us, try to update it
					cnt = update(c,
						"update latest set value = ?, deletePath = ?, acl = ?, creator = ?, vts = ?, cts = ? where key = ? and vts < ?",
						value.data, value.isDeleted, value.acl.id(), value.creator, value.vts, value.cts, key.string, value.vts);
				}

				if (cnt > 0) {
					synchronized (subscriptions) {
						for (final Subscription e : subscriptions) {
							e.call(value);
						}
					}
					return true;
				} else {
					return false;
				}
			}
		}
	}

	private static final int GET_LIMIT = 100;

	Long getLocal(final Subscription subscription) throws SQLException {
		long count = 0;
		try (final Connection c = ds.getConnection()) {
			long maxVts = 0;
			while (true) {
				try (ResultSet rs = query(
					c,
					"SELECT key,value,deletePath,acl,creator,cts,vts FROM latest WHERE vts > ? ORDER BY vts limit ?",
					maxVts, GET_LIMIT
				)) {
					long n = 0;
					while (rs.next()) {
						n++;
						count++;
						maxVts = rs.getLong(7);
						subscription.call(Value.of(
							Key.of(rs.getString(1)),
							rs.getString(2),
							rs.getBoolean(3),
							rs.getString(4),
							rs.getString(5),
							rs.getLong(6),
							maxVts
						));
					}
					if (n != GET_LIMIT) return count;
				}
			}
		}

	}

}
