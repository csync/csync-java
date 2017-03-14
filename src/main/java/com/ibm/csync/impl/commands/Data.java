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

import com.ibm.csync.impl.Envelope;

import static com.ibm.csync.impl.CSyncImpl.gson;

public class Data {

	public static class Response {
		public String[] path;
		public String data;
		public Boolean deletePath;
		public String acl;
		public String creator;
		public Long cts;
		public Long vts;

		public static Response of(final Envelope env) {
			return gson.fromJson(env.payload, Data.Response.class);
		}
	}

	private Data() {}
}
