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
import com.ibm.csync.functional.Futur;
import com.ibm.csync.functional.Promise;
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
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


import static com.ibm.csync.impl.CSyncImpl.gson;

public class Transport implements WebSocketListener {

	private final static Logger logger = LoggerFactory.getLogger(Transport.class);

	private Promise<WebSocket> socketPromise = null;
	private Futur<WebSocket> socketFutur = null;
	private Promise<Boolean> finshedClosing = null;
	private final Executor sendExec = Executors.newSingleThreadExecutor();
	private WebSocket loginWebSocket = null; //Needs to be stored so that we callback when we are sure auth succeeded
	// TODO: handle concurrency
	//private final Set<Callback<WebSocket>> waitingForSocket = ConcurrentHashMap.newKeySet();
	private final Map<Long, Promise<Envelope>> waitingForResponse = Collections.synchronizedMap(new WeakHashMap<>());

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

	public synchronized Futur<Boolean> startSession(String provider, String token) {
		String authURL = this.req.url().toString() + encodeAuthParameters(provider, token);
		Promise<Boolean> sessionPromise = new Promise<>();
		if(socketFutur != null) {
			// Already logged in
			logger.warn("Start session called while the session is already active");
			sessionPromise.set(null, true);
		}
		else {
			connect(authURL)
					.onComplete(workers, (ex, ws) -> {
								if (ex != null) {
									sessionPromise.set(ex,false);
								}
								else {
									sessionPromise.set(null,true);
								}
							}

					);
		}
		return sessionPromise.futur;
	}

	public synchronized Futur<Boolean> endSession() {
		if(socketFutur == null) {
			// Already logged out
			logger.warn("End session called while the session is already not active");
			Promise<Boolean> willLogout = new Promise<>();
			Futur<Boolean>  logoutSuccess = willLogout.futur;
			willLogout.set(null, true);
			return logoutSuccess;
		}
		else {
			if(finshedClosing != null) {
				return finshedClosing.futur;
			}
			finshedClosing = new Promise<>();
			socketFutur.consume(ws -> ws.close(1000,"session ended"));
			return finshedClosing.futur;
		}
	}

	private synchronized Futur<WebSocket> connect(String url) {
		Request req = new Request.Builder()
				.get()
				.url(url)
				.build();
		if (socketFutur == null) {
			socketPromise = new Promise<>(workers);
			socketFutur = socketPromise.futur;
			WebSocketCall.create(client, req).enqueue(this);
		}
		return socketFutur;
	}

	private String encodeAuthParameters(String provider, String token) {
		try {
			return "&authProvider=" + URLEncoder.encode(provider,"UTF-8")
					+ "&token=" + URLEncoder.encode(token,"UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	public synchronized  <T> Futur<T> rpc(final String kind, final Object request, Class<T> cls, final Deadline dl) {
		if(socketFutur == null) {
			// Unauthenticated
			Promise<T> failedPromise = new Promise<>();
			failedPromise.set(new Exception("Unauthorized"), null);
			return failedPromise.futur;
		}
		final Envelope requestEnv = new Envelope(kind, gson.toJsonTree(request));
		final Long closure = requestEnv.closure;
		final String outgoing = gson.toJson(requestEnv);
		logger.debug("outgoing {}", outgoing);

		final Promise<Envelope> responseEnvelopePromise = new Promise<>(workers);
		waitingForResponse.put(closure, responseEnvelopePromise);

		return socketFutur
			.consume(sendExec,ws -> ws.sendMessage(RequestBody.create(WebSocket.TEXT, outgoing)))
			.then(() -> responseEnvelopePromise.futur)
			.map(env -> gson.fromJson(env.payload,cls))
			.deadline(workers,dl)
			.onComplete(workers,(e,res) -> waitingForResponse.remove(closure));
	}

	@Override
	public synchronized void onOpen(WebSocket webSocket, Response response) {
		if (socketPromise == null) {
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
		final Promise<WebSocket> thePromise = socketPromise;
		socketPromise = null;
		socketFutur = null;

		tracer.onError(e,"web socket connection failure %s",response);

		Promise.set(thePromise,e,null);
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
				Promise.set(waitingForResponse.remove(env.closure),null,env);
			} else if ("data".equals(env.kind)) {
				Futur.of(workers, () -> db.set(Value.of(env)))
					.onError(e -> tracer.onError(e,"set"));
			} else if ("connectResponse".equals(env.kind)) {
				final Connect.Response r = gson.fromJson(env.payload, Connect.Response.class);
				// TODO: check uuid
				tracer.onConnect(r);

				//Auth was successful and we are waiting on the callback, so send it
				if(loginWebSocket != null ) {
					final Promise<WebSocket> thePromise = socketPromise;
					socketPromise = null;
					thePromise.set(null, loginWebSocket);
					loginWebSocket = null;
				}

			} else {
				tracer.onError(new Exception(),"unknown kind %s",env);
				//If we failed login and are waiting on a callback, send a failure.
				if(loginWebSocket != null){
					final Promise<WebSocket> thePromise = socketPromise;
					socketPromise = null;
					thePromise.set(new Exception("Auth failed"),loginWebSocket);
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
		final Promise<WebSocket> thePromise = socketPromise;
		socketPromise = null;
		socketFutur = null;
		loginWebSocket = null;
		if (thePromise != null) {
			thePromise.set(new Exception(reason),null);
		}
		if (finshedClosing != null) {
			finshedClosing.set(null, true);
		}
	}
}
