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

import java.util.Date;

public class Connect {

    public static class Response {
    	String uuid;
		String uid;
		Long expires;

		@Override
		public String toString() {
			return String.format("Connect.Response(uid:%s,uuid:%s,expires:%s)",
				uid,
				uuid,
				new Date(expires));
		}
	}

	private Connect() {}
}
