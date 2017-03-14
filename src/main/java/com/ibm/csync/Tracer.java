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


import com.ibm.csync.impl.commands.Connect;
import com.ibm.csync.impl.commands.Data;

public interface Tracer {

	void onError(final Throwable ex, String format, Object... args);
	void onData(final Data.Response data);
	void onConnect(final Connect.Response connect);


	class Helper implements Tracer {


		@Override
		public void onError(Throwable ex, final String format, Object... args) {
			System.err.printf(format,args);
			ex.printStackTrace();
		}

		@Override
		public void onData(Data.Response data) {

		}

		@Override
		public void onConnect(Connect.Response connect) {
			System.err.printf("connect %s\n",connect);
		}
	}
}
