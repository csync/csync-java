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

public interface Acl {
	String id();

	Acl publicCreate();
	Acl publicRead();
	Acl publicUpdate();

	Acl privateCreate();
	Acl privateRead();
	Acl privateUpdate();

	static String id(Acl acl) {
		if (acl == null) return null;
		return acl.id();
	}

	static Acl of(final String id) {
		final Acl acl = Acls.of(id);
		if (acl != null) return acl;

		System.out.printf("*** %s\n",String.valueOf(id));
		System.exit(0);
		return null;
/*
		return new Acl() {

			@Override
			public String id() {
				return (id == null) ? "?" : id;
			}

			@Override
			public Acl publicCreate() {
				return this;
			}

			@Override
			public Acl publicRead() {
				return this;
			}

			@Override
			public Acl publicUpdate() {
				return this;
			}

			@Override
			public Acl privateCreate() {
				return this;
			}

			@Override
			public Acl privateRead() {
				return this;
			}

			@Override
			public Acl privateUpdate() {
				return this;
			}
		};
		*/
	}
}
