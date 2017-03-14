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
import com.ibm.csync.functional.Futur;
import com.ibm.csync.impl.CSyncImpl;

public class Advance {

	static class Request {
		private final Long rvts;
		private final String[] pattern;

		private Request(final String[] pattern, final Long rvts) {
			this.pattern = pattern;
			this.rvts = rvts;
		}

		@Override
		public String toString() {
			return String.format("%s(pattern:%s,rvts:%d)",getClass().getSimpleName(),Key.of(pattern).string,rvts);
		}
	}

	public static class Response {
		public Long vts[];
		public Long maxvts;
	}

    public static Futur<Response> send(CSyncImpl csync, final Key pattern, final Long rvts, final Deadline dl) {
        return csync.ws.rpc(
            "advance",
			new Request(pattern.array,rvts),
			Response.class,
			dl);
    }



    private Advance() {}

}
