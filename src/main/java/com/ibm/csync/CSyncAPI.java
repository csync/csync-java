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

import com.ibm.csync.functional.Futur;
import okhttp3.ws.WebSocket;

import java.io.IOError;
import java.io.IOException;

interface CSyncAPI {

	Futur<Boolean> authenticate(String provider, String token);
	Futur<Boolean> unauthenticate();
	Futur<Long> pub(Key key, String data, Acl acl, Deadline dl);
	Futur<Long> del(final Key key, final Deadline dl);
	Timeout defaultTimeout();

	/////////
	// Pub //
	/////////

	default Futur<Long> pub(final String key, final String data, final Acl acl, final Deadline dl) {
		return pub(Key.of(key),data,acl,dl);
	}

	default Futur<Long> pub(final Key key, final String data, final Acl acl, final Timeout to) {
		return pub(key,data,acl,Deadline.of(to));
	}

	default Futur<Long> pub(final String key, final String data, final Acl acl, final Timeout to) {
		return pub(Key.of(key),data,acl, to);
	}

	default Futur<Long> pub(final Key key, final String data, final Acl acl) {
		return pub(key,data,acl,defaultTimeout());
	}

	default Futur<Long> pub(final String key, final String data, final Acl acl) {
		return pub(Key.of(key),data,acl);
	}

	default Futur<Long> pub(final Key key, final String data, final Deadline dl) {
		return pub(key,data,null,dl);
	}

	default Futur<Long> pub(final String key, final String data, final Deadline dl) {
		return pub(Key.of(key),data,dl);
	}

	default Futur<Long> pub(final Key key, final String data, final Timeout to) {
		return pub(key,data,Deadline.of(to));
	}

	default Futur<Long> pub(final String key, final String data, final Timeout to) {
		return pub(Key.of(key),data, to);
	}

	default Futur<Long> pub(final Key key, final String data) {
		return pub(key,data,(Acl)null);
	}

	default Futur<Long> pub(final String key, final String data) {
		return pub(Key.of(key),data);
	}

	/////////
	// del //
	/////////

	default Futur<Long> del(final String key, final Deadline dl) {
		return del(Key.of(key),dl);
	}

	default Futur<Long> del(final Key key, final Timeout to) {
		return del(key,Deadline.of(to));
	}

	default Futur<Long> del(final String key, final Timeout to) {
		return del(Key.of(key),to);
	}

	default Futur<Long> del(final Key key) {
		return del(key, defaultTimeout());
	}

	default Futur<Long> del(final String key) {
		return del(Key.of(key));
	}

	//////////////
	// Callback //
	//////////////

	// Callback API

	default void pub(final Key key, final String data, final Acl acl, final Deadline dl, Callback<? super Long> cb) {
		pub(key, data, acl, dl).onComplete(cb);
	}

	default void pub(final String key, final String data, final Acl acl, final Deadline dl, Callback<? super Long> cb) {
		pub(key,data,acl,dl).onComplete(cb);
	}

	default void pub(final Key key, final String data, final Acl acl, final Timeout to, Callback<? super Long> cb) {
		pub(key,data,acl,to).onComplete(cb);
	}

	default void pub(final String key, final String data, final Acl acl, final Timeout to, Callback<? super Long> cb) {
		pub(key,data,acl,to).onComplete(cb);
	}

	default void pub(final Key key, final String data, final Acl acl, Callback<? super Long> cb) {
		pub(key,data,acl).onComplete(cb);
	}

	default void pub(final String key, final String data, final Acl acl, Callback<? super Long> cb) {
		pub(key, data, acl).onComplete(cb);
	}


	default void pub(final Key key, final String data, final Deadline dl, Callback<? super Long> cb) {
		pub(key, data, null, dl, cb);
	}

	default void pub(final String key, final String data, final Deadline dl, Callback<? super Long> cb) {
		pub(key,data,dl).onComplete(cb);
	}

	default void pub(final Key key, final String data, final Timeout to, Callback<? super Long> cb) {
		pub(key,data,to).onComplete(cb);
	}

	default void pub(final String key, final String data, final Timeout to, Callback<? super Long> cb) {
		pub(key,data,to).onComplete(cb);
	}

	default void pub(final Key key, final String data, Callback<? super Long> cb) {
		pub(key,data).onComplete(cb);
	}

	default void pub(final String key, final String data, Callback<? super Long> cb) {
		pub(key, data).onComplete(cb);
	}

	// del

	default void del(final Key key, final Deadline dl, Callback<? super Long> cb) {
		del(key,dl).onComplete(cb);
	}

	default void del(final String key, final Deadline dl, Callback<? super Long> cb) {
		del(key,dl).onComplete(cb);
	}

	default void del(final Key key, final Timeout to, Callback<? super Long> cb) {
		del(key,to).onComplete(cb);
	}

	default void del(final String key, final Timeout to, Callback<? super Long> cb) {
		del(key,to).onComplete(cb);
	}

	default void del(final Key key, Callback<? super Long> cb) {
		del(key).onComplete(cb);
	}

	default void del(final String key, Callback<? super Long> cb) {
		del(key).onComplete(cb);
	}
}
