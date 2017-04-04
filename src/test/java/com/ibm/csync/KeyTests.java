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

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class KeyTests {

	@Test
	public void testIdentity() {
		assertSame(Key.of("a.b"), Key.of("a","b"));
	}

	void match(final Key a, final Key b) {
		assertTrue(a.matches(b));
		assertTrue(b.matches(a));
	}

	void match(final String a, final String b) {
		match(Key.of(a),Key.of(b));
	}

	void nomatch(final Key a, final Key b) {
		assertFalse(a.matches(b));
		assertFalse(b.matches(a));
	}

	void nomatch(final String a, final String b) {
		nomatch(Key.of(a),Key.of(b));
	}

	@Test
	public void testMatches() {
		match("a","a");
		match("a","#");
		match("a.b","a.b.#");
		nomatch("a.b","x.*");
	}

	public void testInitializer(){
		Key keyOne = Key.of("a","b","c");
		Key keyTwo = Key.of("a.b.c");
		assertTrue(keyOne.matches(keyTwo));
	}
}
