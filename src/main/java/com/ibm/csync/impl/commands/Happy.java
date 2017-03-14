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

import com.ibm.csync.ServerException;

public class Happy {

	public static class Response {
		Integer code;
		String msg;
		Long cts;
		Long vts;

		@Override
		public String toString() {
			return String.format("Happy(code:%d,msg:%s,cts:%d,vts:%d)", code, msg, cts, vts);
		}

		public Response check() throws ServerException {
			if (code != 0) throw new ServerException(code,msg);
			return this;
		}
	}


	private Happy() {}
}
