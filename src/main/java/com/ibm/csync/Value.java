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

import com.ibm.csync.impl.Envelope;
import com.ibm.csync.impl.commands.Data;

import java.util.Date;

public class Value {
	public final Key key;
	public final String data;
	public final boolean isDeleted;
	public final Acl acl;
	public final String creator;
	public final long cts;
	public final long vts;

	private Value(final Key key, final String data, final boolean isDeleted, final String acl, final String creator, final long cts, final long vts) {
		this.key = key;
		this.data = data;
		this.isDeleted = isDeleted;
		this.acl = Acl.of(acl);
		this.creator = creator;
		this.cts = cts;
		this.vts = vts;
	}

	public static Value of(Data.Response res) {
		return new Value(Key.of(res.path),
			res.data,
			res.deletePath,
			res.acl,
			res.creator,
			res.cts,
			res.vts);
	}

	public static Value of(final Envelope env) {
		return of(Data.Response.of(env));
	}

	public static Value of(final Key key, final String data, final boolean isDeleted, final String acl, final String creator, final long cts, final long vts) {
		return new Value(key,data,isDeleted,acl,creator,cts,vts);
	}

	@Override
	public String toString() {
		return String.format("Value(key:%s,data:%s,isDeleted:%s,acl:%s,creator:%s,cts:%s,vts:%d)",
			key.string,data,isDeleted,acl.id(),creator,new Date(cts),vts);
	}

}
