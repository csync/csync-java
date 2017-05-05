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

import com.ibm.csync.Acl;
import com.ibm.csync.Deadline;
import com.ibm.csync.Key;
import com.ibm.csync.ServerException;
import com.ibm.csync.impl.CSyncImpl;
import com.ibm.csync.impl.CTS;

import java.util.concurrent.CompletableFuture;

public class Pub {

	public static class Request {
		final String path[];
		final Long cts;
		final String data;
		final Boolean deletePath;
		final String assumeACL;


		private Request(String path[], Boolean deletePath, String data, String assumeACL) {
			this.path = path;
			this.deletePath = deletePath;
			this.cts = CTS.next();
			this.data = data;
			this.assumeACL = assumeACL;
		}
	}

    public static CompletableFuture<Long> send(final CSyncImpl impl,
											   final Key key,
											   final Boolean deletePath,
											   final String data,
											   final Acl acl,
											   final Deadline dl) {
		final Request pub = new Request(key.array, deletePath, data, Acl.id(acl));

		return impl.ws.rpc("pub",pub,Happy.Response.class,dl)
			.thenApply(h -> {
				try {
					return h.check().vts;
				} catch (ServerException e) {
					throw new RuntimeException(e);
				}
			});
	}
}
