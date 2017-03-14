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

import java.util.HashMap;
import java.util.Map;

public enum Acls implements Acl {

	Private("$private") {

		@Override
		public Acl publicCreate() {
			return PublicCreate;
		}

		@Override
		public Acl publicRead() {
			return PublicRead;
		}

		@Override
		public Acl publicUpdate() {
			return PublicUpdate;
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
	},

	Public("$public") {
		@Override
		public String id() {
			return "$public";
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
			return PublicReadUpdate;
		}

		@Override
		public Acl privateRead() {
			return PublicCreateUpdate;
		}

		@Override
		public Acl privateUpdate() {
			return PublicCreateRead;
		}
	},

	PublicCreate("$publicCreate") {
		@Override
		public Acl publicCreate() {
			return this;
		}

		@Override
		public Acl publicRead() {
			return PublicCreateRead;
		}

		@Override
		public Acl publicUpdate() {
			return PublicCreateUpdate;
		}

		@Override
		public Acl privateCreate() {
			return Private;
		}

		@Override
		public Acl privateRead() {
			return this;
		}

		@Override
		public Acl privateUpdate() {
			return PublicCreate;
		}
	},

	PublicRead("$publicRead") {
		@Override
		public Acl publicCreate() {
			return PublicCreateRead;
		}

		@Override
		public Acl publicRead() {
			return this;
		}

		@Override
		public Acl publicUpdate() {
			return PublicReadUpdate;
		}

		@Override
		public Acl privateCreate() {
			return this;
		}

		@Override
		public Acl privateRead() {
			return Private;
		}

		@Override
		public Acl privateUpdate() {
			return this;
		}
	},

	PublicUpdate("$publicUpdate") {
		@Override
		public Acl publicCreate() {
			return PublicCreateUpdate;
		}

		@Override
		public Acl publicRead() {
			return PublicReadUpdate;
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
			return Private;
		}
	},

	PublicCreateRead("$publicCreateRead") {
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
			return PublicCreateReadUpdate;
		}

		@Override
		public Acl privateCreate() {
			return PublicRead;
		}

		@Override
		public Acl privateRead() {
			return PublicCreate;
		}

		@Override
		public Acl privateUpdate() {
			return this;
		}
	},

	PublicCreateUpdate("$publicCreateUpdate") {
		@Override
		public Acl publicCreate() {
			return this;
		}

		@Override
		public Acl publicRead() {
			return PublicCreateReadUpdate;
		}

		@Override
		public Acl publicUpdate() {
			return this;
		}

		@Override
		public Acl privateCreate() {
			return PublicUpdate;
		}

		@Override
		public Acl privateRead() {
			return this;
		}

		@Override
		public Acl privateUpdate() {
			return PublicCreate;
		}
	},

	PublicReadUpdate("$publicReadUpdate") {
		@Override
		public Acl publicCreate() {
			return PublicCreateReadUpdate;
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
			return PublicUpdate;
		}

		@Override
		public Acl privateUpdate() {
			return PublicRead;
		}
	},

	PublicCreateReadUpdate("$publicCreateReadUpdate") {
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
			return PublicReadUpdate;
		}

		@Override
		public Acl privateRead() {
			return PublicCreateUpdate;
		}

		@Override
		public Acl privateUpdate() {
			return PublicCreateRead;
		}
	};

	static Map<String,Acl> byName = new HashMap<>();
	static {
		for (Acl a : Acls.values()) {
			byName.put(a.id(),a);
		}
	}

	private final String id;

	Acls(final String id) {
		this.id = id;
	}

	@Override
	public String id() {
		return id;
	}

	public static Acl of(final String id) {
		return byName.get(id);
	}
}
