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

import com.ibm.csync.Deadline;
import com.ibm.csync.Tracer;
import com.ibm.csync.Value;
import com.ibm.csync.impl.commands.Connect;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.ws.WebSocket;
import okhttp3.ws.WebSocketCall;
import okhttp3.ws.WebSocketListener;
import okio.Buffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.*;


import static com.ibm.csync.impl.CSyncImpl.gson;

public class Transport implements WebSocketListener {

	private final static Logger logger = LoggerFactory.getLogger(Transport.class);

	private CompletableFuture<WebSocket> socketFuture = null;
	private CompletableFuture<Void> finshedClosing = null;
	private final Executor sendExec = Executors.newSingleThreadExecutor();
	private WebSocket loginWebSocket = null; //Needs to be stored so that we callback when we are sure auth succeeded
	// TODO: handle concurrency
	//private final Set<Callback<WebSocket>> waitingForSocket = ConcurrentHashMap.newKeySet();
	private final Map<Long, CompletableFuture<Envelope>> waitingForResponse = Collections.synchronizedMap(new WeakHashMap<>());

	private final Tracer tracer;

	private final Database db;
	private final ScheduledExecutorService workers;

	private Request req;
	private final OkHttpClient client;

	Transport(final String url, final Database db, final ScheduledExecutorService workers, final Tracer tracer) {
		//this.url = url;
		this.db = db;
		this.workers = workers;
		this.tracer = tracer;

		req = new Request.Builder()
				.get()
				.url(url)
				.build();

		client = new OkHttpClient.Builder()
			.readTimeout(60, TimeUnit.SECONDS)
			.writeTimeout(60, TimeUnit.SECONDS).build();

	}

	public synchronized CompletableFuture<Void> startSession(String provider, String token) {
		String authURL = this.req.url().toString() + encodeAuthParameters(provider, token);
		CompletableFuture<Void>  sessionPromise = new CompletableFuture<>();
		if(socketFuture != null) {
			// Already logged in
			logger.warn("Start session called while the session is already active");
			sessionPromise.complete(null);
		}
		else {
			connect(authURL)
					.whenCompleteAsync((ws, ex) -> {
								if (ex != null) {
									sessionPromise.completeExceptionally(ex);
								}
								else {
									sessionPromise.complete(null);
								}
							}
						, workers

					);
		}
		return sessionPromise;
	}

	public synchronized CompletableFuture<Void> endSession() {
		if(socketFuture == null) {
			// Already logged out
			logger.warn("End session called while the session is already not active");
			return CompletableFuture.completedFuture(null);
		}
		else {
			if(finshedClosing != null) {
				return finshedClosing;
			}
			finshedClosing = new CompletableFuture<>();
			socketFuture.whenComplete((ws, ex) -> {
				if(ex != null) {
					throw new RuntimeException(ex);
				}
				try {
					ws.close(1000, "session ended");
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			return finshedClosing;
		}
	}

	private synchronized CompletableFuture<WebSocket> connect(String url) {
		Request req = new Request.Builder()
				.get()
				.url(url)
				.build();
		if (socketFuture == null) {
			socketFuture = new CompletableFuture<>();
			WebSocketCall.create(client, req).enqueue(this);
		}
		return socketFuture;
	}

	private String encodeAuthParameters(String provider, String token) {
		try {
			return "&authProvider=" + URLEncoder.encode(provider,"UTF-8")
					+ "&token=" + URLEncoder.encode(token,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized  <T> CompletableFuture<T> rpc(final String kind, final Object request, Class<T> cls, final Deadline dl) {
		if(socketFuture == null) {
			// Unauthenticated
			CompletableFuture<T> failedPromise = new CompletableFuture<>();
			failedPromise.completeExceptionally(new Exception("Unauthorized"));
			return failedPromise;
		}
		final Envelope requestEnv = new Envelope(kind, gson.toJsonTree(request));
		final Long closure = requestEnv.closure;
		final String outgoing = gson.toJson(requestEnv);
		logger.debug("outgoing {}", outgoing);

		final CompletableFuture<Envelope> responseEnvelopePromise = new CompletableFuture<>();
		waitingForResponse.put(closure, responseEnvelopePromise);

		return socketFuture
			.thenApplyAsync(ws -> {
				try {
					ws.sendMessage(RequestBody.create(WebSocket.TEXT, outgoing));
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				return ws;
			}, sendExec)
			.thenApplyAsync((ws) -> {
				try {
					return responseEnvelopePromise.get(dl.ms, TimeUnit.MILLISECONDS);
				} catch (Exception e) {
					throw new RuntimeException(e);
				}
			}, workers)
			.thenApplyAsync(env -> gson.fromJson(env.payload,cls), workers)
			.whenCompleteAsync((e,res) -> waitingForResponse.remove(closure), workers);
	}

	@Override
	public synchronized void onOpen(WebSocket webSocket, Response response) {
		if (socketFuture == null) {
			try {
				webSocket.close(0,"not needed");
			} catch (IOException e) {
				tracer.onError(e, "onOpen");
			}
		}
		//set login websocket to callback if it isn't already set
		if(loginWebSocket == null)
			loginWebSocket = webSocket;
	}

	@Override
	public synchronized void onFailure(IOException e, Response response) {
		final CompletableFuture<WebSocket> theFuture = socketFuture;
		socketFuture = null;

		tracer.onError(e,"web socket connection failure %s",response);

		theFuture.completeExceptionally(e);
	}

	@Override
	public void onMessage(final ResponseBody message_) throws IOException {
		try (final ResponseBody message = message_) {
			final MediaType type = message.contentType();
			if (type != WebSocket.TEXT) {
				tracer.onError(new Exception(), "don't know how to handle binary");
			}

			final Envelope env = gson.fromJson(message.string(), Envelope.class);
			//logger.info("{}",env);
			if (env.closure != null) {
				waitingForResponse.remove(env.closure).complete(env);
			} else if ("data".equals(env.kind)) {
				CompletableFuture.runAsync(() -> {
					try {
						db.set(Value.of(env));
					} catch (SQLException e) {
						tracer.onError(e,"set");
					}
				}, workers);
			} else if ("connectResponse".equals(env.kind)) {
				final Connect.Response r = gson.fromJson(env.payload, Connect.Response.class);
				// TODO: check uuid
				tracer.onConnect(r);

				//Auth was successful and we are waiting on the callback, so send it
				if(loginWebSocket != null ) {
					socketFuture.complete(loginWebSocket);
					loginWebSocket = null;
				}

			} else {
				tracer.onError(new Exception(),"unknown kind %s",env);
				//If we failed login and are waiting on a callback, send a failure.
				if(loginWebSocket != null){
					final CompletableFuture<WebSocket> theFuture = socketFuture;
					socketFuture = null;
					theFuture.completeExceptionally(new Exception("Auth failed"));
					loginWebSocket = null;
				}
			}
		}
	}

	@Override
	public void onPong(Buffer payload) {

	}

	@Override
	public synchronized void onClose(int code, String reason) {
		final CompletableFuture<WebSocket> theFuture = socketFuture;
		socketFuture = null;
		loginWebSocket = null;
		if (theFuture != null) {
			theFuture.completeExceptionally(new Exception(reason));
		}
		if (finshedClosing != null) {
			finshedClosing.complete(null);
		}
	}
}
