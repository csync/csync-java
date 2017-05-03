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

package com.ibm.csync.impl.commands;

import com.ibm.csync.Deadline;
import com.ibm.csync.Key;
import com.ibm.csync.ServerException;
import com.ibm.csync.impl.CSyncImpl;

import java.util.concurrent.CompletableFuture;


public class Sub {
	public static class Request {
		public final String[] path;

		private Request(final String[] path) {
			this.path = path;
		}
	}

	public static CompletableFuture<Boolean> send(final CSyncImpl csync, final Key key, final Deadline dl)  {
		return csync.ws.rpc("sub",new Request(key.array),Happy.Response.class, dl)
			.thenApply(h -> {
				try {
					h.check();
				} catch (ServerException e) {
					throw new RuntimeException(e);
				}
				return true;
			});
	}
}
