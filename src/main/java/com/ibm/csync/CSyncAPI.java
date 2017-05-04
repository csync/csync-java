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

import okhttp3.ws.WebSocket;

import java.io.IOError;
import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

interface CSyncAPI {

	CompletableFuture<Void> authenticate(String provider, String token);
	CompletableFuture<Void> unauthenticate();
	CompletableFuture<Long> pub(Key key, String data, Acl acl, Deadline dl);
	CompletableFuture<Long> del(final Key key, final Deadline dl);
	Timeout defaultTimeout();

	/////////
	// Pub //
	/////////

	default CompletableFuture<Long> pub(final String key, final String data, final Acl acl, final Deadline dl) {
		return pub(Key.of(key),data,acl,dl);
	}

	default CompletableFuture<Long> pub(final Key key, final String data, final Acl acl, final Timeout to) {
		return pub(key,data,acl,Deadline.of(to));
	}

	default CompletableFuture<Long> pub(final String key, final String data, final Acl acl, final Timeout to) {
		return pub(Key.of(key),data,acl, to);
	}

	default CompletableFuture<Long> pub(final Key key, final String data, final Acl acl) {
		return pub(key,data,acl,defaultTimeout());
	}

	default CompletableFuture<Long> pub(final String key, final String data, final Acl acl) {
		return pub(Key.of(key),data,acl);
	}

	default CompletableFuture<Long> pub(final Key key, final String data, final Deadline dl) {
		return pub(key,data,null,dl);
	}

	default CompletableFuture<Long> pub(final String key, final String data, final Deadline dl) {
		return pub(Key.of(key),data,dl);
	}

	default CompletableFuture<Long> pub(final Key key, final String data, final Timeout to) {
		return pub(key,data,Deadline.of(to));
	}

	default CompletableFuture<Long> pub(final String key, final String data, final Timeout to) {
		return pub(Key.of(key),data, to);
	}

	default CompletableFuture<Long> pub(final Key key, final String data) {
		return pub(key,data,(Acl)null);
	}

	default CompletableFuture<Long> pub(final String key, final String data) {
		return pub(Key.of(key),data);
	}

	/////////
	// del //
	/////////

	default CompletableFuture<Long> del(final String key, final Deadline dl) {
		return del(Key.of(key),dl);
	}

	default CompletableFuture<Long> del(final Key key, final Timeout to) {
		return del(key,Deadline.of(to));
	}

	default CompletableFuture<Long> del(final String key, final Timeout to) {
		return del(Key.of(key),to);
	}

	default CompletableFuture<Long> del(final Key key) {
		return del(key, defaultTimeout());
	}

	default CompletableFuture<Long> del(final String key) {
		return del(Key.of(key));
	}

	//////////////
	// Callback //
	//////////////

	// Callback API

	default void pub(final Key key, final String data, final Acl acl, final Deadline dl, BiConsumer<? super Long, ? super Throwable> cb) {
		pub(key, data, acl, dl).whenComplete(cb);
	}

	default void pub(final String key, final String data, final Acl acl, final Deadline dl, BiConsumer<? super Long, ? super Throwable> cb) {
		pub(key,data,acl,dl).whenComplete(cb);
	}

	default void pub(final Key key, final String data, final Acl acl, final Timeout to, BiConsumer<? super Long, ? super Throwable> cb) {
		pub(key,data,acl,to).whenComplete(cb);
	}

	default void pub(final String key, final String data, final Acl acl, final Timeout to, BiConsumer<? super Long, ? super Throwable> cb) {
		pub(key,data,acl,to).whenComplete(cb);
	}

	default void pub(final Key key, final String data, final Acl acl, BiConsumer<? super Long, ? super Throwable> cb) {
		pub(key,data,acl).whenComplete(cb);
	}

	default void pub(final String key, final String data, final Acl acl, BiConsumer<? super Long, ? super Throwable> cb) {
		pub(key, data, acl).whenComplete(cb);
	}


	default void pub(final Key key, final String data, final Deadline dl, BiConsumer<? super Long, ? super Throwable> cb) {
		pub(key, data, null, dl, cb);
	}

	default void pub(final String key, final String data, final Deadline dl, BiConsumer<? super Long, ? super Throwable> cb) {
		pub(key,data,dl).whenComplete(cb);
	}

	default void pub(final Key key, final String data, final Timeout to, BiConsumer<? super Long, ? super Throwable> cb) {
		pub(key,data,to).whenComplete(cb);
	}

	default void pub(final String key, final String data, final Timeout to, BiConsumer<? super Long, ? super Throwable> cb) {
		pub(key,data,to).whenComplete(cb);
	}

	default void pub(final Key key, final String data, BiConsumer<? super Long, ? super Throwable> cb) {
		pub(key,data).whenComplete(cb);
	}

	default void pub(final String key, final String data, BiConsumer<? super Long, ? super Throwable> cb) {
		pub(key, data).whenComplete(cb);
	}

	// del

	default void del(final Key key, final Deadline dl, BiConsumer<? super Long, ? super Throwable> cb) {
		del(key,dl).whenComplete(cb);
	}

	default void del(final String key, final Deadline dl, BiConsumer<? super Long, ? super Throwable> cb) {
		del(key,dl).whenComplete(cb);
	}

	default void del(final Key key, final Timeout to, BiConsumer<? super Long, ? super Throwable> cb) {
		del(key,to).whenComplete(cb);
	}

	default void del(final String key, final Timeout to, BiConsumer<? super Long, ? super Throwable> cb) {
		del(key,to).whenComplete(cb);
	}

	default void del(final Key key, BiConsumer<? super Long, ? super Throwable> cb) {
		del(key).whenComplete(cb);
	}

	default void del(final String key, BiConsumer<? super Long, ? super Throwable> cb) {
		del(key).whenComplete(cb);
	}
}
