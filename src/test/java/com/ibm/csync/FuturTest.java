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

import com.ibm.csync.functional.Func1;
import com.ibm.csync.functional.Futur;
import com.ibm.csync.functional.Promise;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.util.concurrent.*;

import static org.junit.Assert.*;

public class FuturTest {
	@Test
	public void get() throws Exception {
		assertEquals(10, Futur.success(10).get(Timeout.of(1000)).intValue());

		final Exception e = new Exception();
		try {
			Futur.failure(e).get(Timeout.of(1000000));
			fail("should not get here");
		} catch (Exception e1) {
			assertEquals(e,e1);
		}
	}

	@Test
	public void on() throws Exception {
		final ExecutorService e = Executors.newSingleThreadExecutor();
		final Futur<Integer> f = Futur.success(10);
		final String t1 = f.map(x -> Thread.currentThread().getName()).get(Timeout.of(1000));
		final String t2 = f.map(e,x -> Thread.currentThread().getName()).get(Timeout.of(1000));

		assertNotEquals(t1,t2);
		e.shutdownNow();
	}

	@Test
	public void map() throws Exception {
		assertEquals(100,Futur.success("100").map(Integer::parseInt).get(Timeout.of(1000)).intValue());

		Futur.success("xyz").map(Integer::parseInt).onComplete((e,v) -> {
			assertNotNull(e);
			assertNull(v);
		});
	}

	@Test
	public void apply() throws Exception {
		final Func1<Integer,Integer> f = x -> x+1;

		Futur.success(100).apply(Futur.success(f)).onComplete((e,v) -> {
			assertNull(e);
			assertNotNull(v);
			assertEquals(101,v.intValue());
		});
	}

	@Test
	public void flatMap() throws Exception {
		final Exception ex = new Exception();

		Futur.success(100).flatMap(v -> Futur.success(v+1)).onComplete((e,v) -> {
			assertNull(e);
			assertNotNull(v);
			assertEquals(101,v.intValue());
		});
		Futur.success(100).flatMap(v -> Futur.failure(ex)).onComplete((e,v) -> {
			assertNotNull(e);
			assertNull(v);
			assertEquals(ex,e);
		});
		Futur.<Integer>failure(ex).flatMap(v -> Futur.success(v+1)).onComplete((e,v) -> {
			assertNotNull(e);
			assertNull(v);
			assertEquals(ex,e);
		});
		Futur.failure(ex).flatMap(v -> Futur.failure(new Exception())).onComplete((e,v) -> {
			assertNotNull(e);
			assertNull(v);
			assertEquals(ex,e);
		});
	}

	@Test
	public void onComplete() throws Exception {
		Futur.success(1000).onComplete((e,v) -> {
			assertNull(e);
			assertNotNull(v);
			assertEquals(1000,v.intValue());
		});
	}

	@Test
	public void success() throws Exception {
		Futur.success(1000).onComplete((e,v) -> {
			assertNull(e);
			assertNotNull(v);
			assertEquals(1000,v.intValue());
		});
	}

	@Test
	public void failure() throws Exception {
		Futur.of(() -> { throw new Exception(); }).onComplete((e,v) -> {
			assertNotNull(e);
			assertNull(v);
		});
	}


	@Test
	public void apply2() throws Exception {
		Futur.apply2(Futur.success(10),Futur.success(20),(x,y) -> x+y).onComplete((e,v) -> {
			assertNull(e);
			assertNotNull(v);
			assertEquals(30,v.intValue());
		});
	}

	@Test
	public void join() throws Exception {
		Futur.join(Futur.success(Futur.success(10))).onComplete((e,v) -> {
			assertNull(e);
			assertNotNull(v);
			assertEquals(10,v.intValue());
		});
	}

	@Test
	public void compose() throws Exception {
		final Func1<String,Futur<Integer>> parse = s -> Futur.of(() -> Integer.parseInt(s));
		final Func1<Integer,Futur<Integer>> div = i -> Futur.of(() -> 100 / i);
		final Func1<String,Futur<Integer>> together = Futur.compose(parse,div);

		together.call("20").onComplete((e, v) -> {
			assertNull(e);
			assertNotNull(v);
			assertEquals(5,v.intValue());
		});

		together.call("xyz").onComplete((e, v) -> {
			assertNotNull(e);
			assertThat(e, CoreMatchers.instanceOf(NumberFormatException.class));
			assertNull(v);
		});

		together.call("0").onComplete((e, v) -> {
			assertNotNull(e);
			assertThat(e, CoreMatchers.instanceOf(ArithmeticException.class));
			assertNull(v);
		});
	}

	@Test
	public void then() throws Exception {
		Futur
			.success("x")
			.then(() -> Futur.success(10))
			.onComplete((e,v) -> {
				assertNull(e);
				assertNotNull(v);
				assertEquals(10,v.intValue());
			});
	}

	@Test
	public void consume() throws Exception {
		Futur
			.success("10")
			.consume(x -> assertEquals("10",x))
			.map(Integer::parseInt)
			.consume(x -> assertEquals(10,x.intValue()));
	}

	@Test
	public void zip() {
		final Promise<String> promise = new Promise<>();
		final Exception ex = new Exception();

		Futur.zip(promise.futur, Futur.success(10)).onComplete((e,v) -> {
			assertNull(e);
			assertNotNull(v);
			assertEquals("hello",v.a);
			assertEquals(10,v.b.intValue());
		});

		promise.set(null,"hello");

		Futur.zip(promise.futur, Futur.failure(ex)).onComplete((e,v) -> {
			assertEquals(ex,e);
			assertNull(v);
		});

		Futur.zip(Futur.failure(ex),promise.futur).onComplete((e,v) -> {
			assertEquals(ex,e);
			assertNull(v);
		});

		Futur.zip(Futur.failure(ex),Futur.failure(new Exception())).onComplete((e,v) -> {
			assertEquals(ex,e);
			assertNull(v);
		});
	}

	@Test
	public void timeout() {
		final ScheduledExecutorService sched = Executors.newScheduledThreadPool(1);
		final Promise<Long> promise = new Promise<>();

		try {
			promise.futur.timeout(sched, 1, TimeUnit.MICROSECONDS).get(Deadline.of(Long.MAX_VALUE));
			assertTrue(false);
		} catch (Exception ex) {
			assertThat(ex,CoreMatchers.instanceOf(TimeoutException.class));
		}
		sched.shutdownNow();
	}

}