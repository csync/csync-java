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

package com.ibm.csync;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.ibm.csync.functional.Futur;
import com.ibm.csync.impl.CSyncImpl;
import com.ibm.csync.impl.commands.Pub;
import okhttp3.ws.WebSocket;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.UUID;

public class CSync implements CSyncAPI {

	//public static Logger logger = LoggerFactory.getLogger(CSync.class);

	private final CSyncImpl impl;
	private final Builder builder;


	private CSync(final CSyncImpl impl, final Builder builder) throws Exception {
		this.impl = impl;
		this.builder = builder;
	}

	public ScheduledExecutorService workers() {
		return impl.workers;
	}

	// CSyncAPI

	@Override
	public Futur<Long> pub(final Key key, final String data, final Acl acl, final Deadline dl) {
		return Pub.send(impl, key, false, data, acl, dl);
	}

	@Override
	public Timeout defaultTimeout() {
		return builder.defaultBlockingTimeout; // TODO: fix this
	}

	@Override
	public Futur<Long> del(final Key key, final Deadline dl) {
		return Pub.send(impl, key, true, null, null, dl);
	}



	// Auth
	
	@Override
	public Futur<Boolean> authenticate(String provider, String token) {
		return impl.ws.startSession(provider, token);
	}

	@Override
	public Futur<Boolean> unauthenticate() {
		return impl.ws.endSession();
	}

	// Blocking

	public final Blocking blocking = new Blocking() {
		@Override
		public Long pub(Key key, String data, final Acl acl, Deadline dl) throws Exception {
			return CSync.this.pub(key,data,acl,dl).get(dl);
		}

		@Override
		public Timeout defaultTimeout() {
			return builder.defaultBlockingTimeout;
		}

		@Override
		public Long del(Key key, Deadline dl) throws Exception {
			return CSync.this.del(key,dl).get(dl);
		}
	};

	////////////
	// Listen //
	////////////

	public Closeable listen(final Key pattern, final Deadline dl, final Listener cb)  {
		return impl.listen(pattern,dl,cb);
	}

	public Closeable listen(final Key pattern, final Listener cb)  {
		return impl.listen(pattern,Deadline.of(builder.defaultBlockingTimeout),cb);
	}

	/////////////
	// Builder //
	/////////////

	public static class Builder {
		private String protocol = "ws";
		private String host = "127.0.0.1";
		private int port = 6005;
		private String path = "connect";
		private Map<String,String> args = new HashMap<>();
		private Tracer tracer = new Tracer.Helper();
		private ScheduledExecutorService workers = null;
		private Timeout defaultBlockingTimeout = new Timeout(10000);

		private Builder() {}

		public Builder defaultBlockingTimeout(final Timeout timeout) {
			if (timeout == null || timeout.ms < 0) throw new IllegalArgumentException();
			this.defaultBlockingTimeout = timeout;
			return this;
		}

		synchronized public Builder workers(final ScheduledExecutorService workers) {
			if (workers == null) throw new IllegalArgumentException();
			this.workers = workers;
			return this;
		}

		synchronized public ScheduledExecutorService workers() {
			if (workers == null) {
				final ThreadFactory factory = new ThreadFactoryBuilder()
					.setNameFormat("worker-%d")
					.setDaemon(true)
					.setUncaughtExceptionHandler((thread,ex) -> tracer.onError(ex,"uncaught by %s",thread.getName()))
					.build();
				workers = Executors.newScheduledThreadPool(6,factory);
			}
			return workers;
		}


		public synchronized Builder tracer(final Tracer tracer) {
			if (tracer == null) throw new IllegalArgumentException();
			this.tracer = tracer;
			return this;
		}

		public synchronized Tracer tracer() {
			return tracer;
		}

		public Builder protocol(final String protocol) {
			this.protocol = protocol;
			return this;
		}

		public Builder host(final String host) {
			this.host = host;
			return this;
		}

		public Builder port(final int port) {
			this.port = port;
			return this;
		}

		public Builder path(final String path) {
			this.path = path;
			return this;
		}

		private static String encode(final String in) {
			try {
				return URLEncoder.encode(in,"UTF-8");
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}

		public String url() {
			final String params = args.entrySet()
				.stream()
				.map(e -> encode(e.getKey()) + "=" + e.getValue())
				.reduce("?",(a,b) -> a + "&" + b);

			return String.format("%s://%s:%d/%s%s",protocol,host,port,path,params);
		}

		public CSync build() throws Exception {
			final CSyncImpl impl = new CSyncImpl(this);
			return new CSync(impl,this);
		}
	}

	public static Builder builder() {
		Builder builder = new Builder();
		builder.args.put("sessionId",UUID.randomUUID().toString());
		return builder;
	}

	public interface Blocking {
		Long pub(final Key key, final String data, Acl acl, Deadline dl) throws Exception;
		Timeout defaultTimeout();

		default Long pub(final Key key, final String data, Acl acl, Timeout to) throws Exception {
			return pub(key,data,acl,Deadline.of(to));
		}

		default Long pub(final Key key, final String data, Acl acl) throws Exception {
			return pub(key,data,acl,defaultTimeout());
		}

		default Long pub(final String key, final String data, Acl acl, Deadline dl) throws Exception {
			return pub(Key.of(key),data,acl,dl);
		}

		default Long pub(final String key, final String data, Acl acl, Timeout to) throws Exception {
			return pub(Key.of(key),data,acl,to);
		}

		default Long pub(final String key, final String data, final Acl acl) throws Exception {
			return pub(Key.of(key),data,acl,defaultTimeout());
		}

		default Long pub(final Key key, final String data, Deadline dl) throws Exception {
			return pub(key,data,null,dl);
		}

		default Long pub(final Key key, final String data, Timeout to) throws Exception {
			return pub(key,data,null,to);
		}

		default Long pub(final Key key, final String data) throws Exception {
			return pub(key,data,(Acl)null);
		}

		default Long pub(final String key, final String data, Deadline dl) throws Exception {
			return pub(key,data,null,dl);
		}

		default Long pub(final String key, final String data, Timeout to) throws Exception {
			return pub(key,data,null,to);
		}

		default Long pub(final String key, final String data) throws Exception {
			return pub(key,data,(Acl)null);
		}

		// blocking del

		Long del(final Key key, Deadline dl) throws Exception;

		default Long del(final Key key, Timeout to) throws Exception {
			return del(key,Deadline.of(to));
		}

		default Long del(final Key key) throws Exception {
			return del(key,defaultTimeout());
		}

		default Long del(final String key, Deadline dl) throws Exception {
			return del(Key.of(key),dl);
		}

		default Long del(final String key, Timeout to) throws Exception {
			return del(Key.of(key),to);
		}

		default Long del(final String key) throws Exception {
			return del(Key.of(key));
		}
	}
}
