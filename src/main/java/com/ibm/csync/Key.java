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

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import java.util.concurrent.ExecutionException;

public class Key {

    public final String string;
    public final String array[];

    private Key(final String string, final String[] array) {
    	this.array = array;
    	this.string = string;
	}

	private boolean matchesWithLonger(final Key other, final int nShort, final int nLong) {
		for (int i = 0; i < nShort; i++) {
			final String a = this.array[i];
			final String b = other.array[i];
			if ("*".equals(a)) continue;
			if ("*".equals(b)) continue;
			if ("#".equals(a)) return true;
			if ("#".equals(b)) return true;
			if (!a.equals(b)) return false;
		}
		return nLong <= nShort || other.array[nShort].equals("#");
	}

	public boolean matches(final Key other) {
    	if (other == null) return false;
    	if (this == other) return true;
    	final int la = this.array.length;
    	final int lb = other.array.length;
    	if (la > lb) {
    		return other.matchesWithLonger(this, lb, la);
		} else {
    		return this.matchesWithLonger(other, la, lb);
		}
	}

	private static Cache<String,Key> cache = CacheBuilder
		.newBuilder()
		.softValues()
		.build();

	public static Key of(String... parts) {
		final String s = String.join(".",parts);
		try {
			return cache.get(s, () -> new Key(s, s.split("\\.")));
		} catch (ExecutionException ex) {
			throw new RuntimeException(ex);
		}
	}


}
