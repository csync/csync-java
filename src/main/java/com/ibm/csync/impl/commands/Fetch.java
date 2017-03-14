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
import com.ibm.csync.functional.Futur;
import com.ibm.csync.impl.CSyncImpl;

import java.util.List;

public class Fetch {

	static class Request {
		Long vts[];

		private Request(final Long vts[]) {
			this.vts = vts;
		}
		private Request(final List<Long> vts) {
			this.vts = vts.toArray(new Long[0]);
		}
	}

	public static class Response {
		public Data.Response[] response;
	}

    public static Futur<Data.Response[]> send(final CSyncImpl csync, final List<Long> vts, final Deadline dl) {
		if (vts.size() == 0) {
			return Futur.success(new Data.Response[0]);
		} else {
			return csync.ws.rpc(
				"fetch",
				new Request(vts),
				Response.class,dl).map(r -> r.response);
		}
    }

}
