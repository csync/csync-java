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

package com.ibm.csync.functional;

import com.ibm.csync.Deadline;
import com.ibm.csync.Timeout;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

// Java struggles with futures
//    The 1.4 API is silly
//    The 1.8 API is acceptable by overly complex and dances around the monadic nature of futures inventing new terms
//    The Guava futures are ok but force us to think in terms strategies, etc.
//    A "single" RxJava Observable can be thought of as a future but RxJava complicates error handling and switching
//        executors eats exceptions unless you're careful
//    Scala futures are sensible but they have a scary design decision and relying on an implicit ExecutionContext.
//        This is a horrible idea that feels dirty and Java doesn't have implicits anyway. Forcing the API to be
//        riddled with noisy ExecutionContext or their Java counterparts, executors.
//
// This leaves me with no choice but to introduce my own notion of futures which is not really a big deal.
//    A future is a monad (knows how to bind) which is an applicative functor (knows how to call)
//    which is a functor (knows to map)
//
// Implementing your own is not hard but introduces a few challenges:
//
//    - The name Future is taken by Java 1.4. This forced Java8 to go with the ugly name CompletableFuture and
//      Guava to go with ListenableFuture. I didn't want to go with silly prefixes like CSFuture so I decided
//      to go with Avenir, French for future according to google translate. I tried German and Dutch and others
//      but they're all hard
//
//    - What should we call the "bind" operator? Haskell calls it bind but Scala decided to go with "flatMap" and
//      the rest followed. Most people don't understand the power of monads so they'll never use them in generic
//      algorithms (and languages other than Haskell make that unnatural anyway). Most programmers have seen
//      flatMap used in other contexts like RXJava and would be capable of using it usefully without being forced
//      to read about monads and other such things. I decided to stick with flatMap to minimize the confusion
//
//    - How do you control where your code runs is it traverses the "bound" futures? Java and its libraries have
//      a strong bias towards segregating work in dedicating executors. I have mixed feeling about that but I can't
//      fight the trend and any attempt to do so would break existing useful libraries like okhttp, etc.
//
//      I could say that this is the programmers responsibility and they should implement whatever Executor hopping
//      policy they want in their code. This makes the code ugly and hard to write, read, debug, etc.
//
//      This is why Scala has those implicit execution contexts but implicit things are scary at best
//
//      Guava's ListenableFuture has Strategy objects that play the role of the ExecutionContext and more but they
//      obscure the API and expect people to go learn about them
//
//      The RXJava way is sane. You can put observeOn in the middle of the chain and that affects a switch to the
//      required Executor, clean and simple and easy to find and add to the code.
//
//      There is no need to add any new abstraction in order to support that. All we need is a function that maps
//      a value to an Avenir running in a different executor. flatMap with that produces that desired effect. We
//      provide that along with some convenience methods
//
//    - How do we handle errors? Errors propagate cleanly through bound futures but have do we force programmers
//      to deal with them on the edges (get and onComplete) without being obnoxious?
//
//      Scala has onSuccess and onFailure and errors silently disappear if you forget to say on Failure. It also
//      has onComplete which forces you to handle the error but it requires Try. I didn't want to add that but
//      maybe I should
//
//      RXJava has very similar onNext and onError and onComplete but forgetting to but onError in there produces
//      horrible stack traces that are impossible to figure out
//
//      Node.js has the right answer in my opinion, callbacks get two arguments by convention: an error and the result
//      You can opt to ignore the error but it will be staring you in face making you feel guilty as you do so
//
//      The decision was to have an onComplete(Exception ex, T data) and a T get() throws Exception
//          no matter what you do, you'll have this constant reminder that errors should be checked
//
//    - How do you handle timeouts? Java has a convention of passing a deadline argument to methods that care but
//      timeouts don't compose nicely. I call "a" which calls "b" which calls "c", what timeout should each get?
//      Passing a deadline would make sense but a timeout is more natural on the edges. We provide two classes
//      Timeout and Deadline that know how to convert from one to another and programmers are free to use
//      whatever is natural for them.

public interface Futur<A> {

	default A get(Timeout to) throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);

		Ref<A> v_ = new Ref<>(null);
		Ref<Exception> e_ = new Ref<>(null);


		this.onComplete((e,v) -> {
			v_.a = v;
			e_.a = e;
			latch.countDown();
		});

		if (latch.await(to.ms, TimeUnit.MILLISECONDS)) {
			if (e_.a != null) throw e_.a;
			return v_.a;
		} else {
			throw new TimeoutException();
		}
	}

	default A get(Deadline dl) throws Exception {
		return this.get(Timeout.of(dl));
	}

	default <B> Futur<B> map(final Executor exec, final Func1<A,B> f) {
		return flatMap(exec,a -> success(f.call(a)));
	}

	default <B> Futur<B> map(final Func1<A,B> f) {
		return map(CurrentThreadExecutor.it,f);
	}

	default <B> Futur<B> apply(final Futur<Func1<A,B>> func) {
		return flatMap(a -> func.map (f -> f.call(a)));
	}

	default <B> Futur<B> flatMap(Func1<A,Futur<B>> f) {
		return flatMap(CurrentThreadExecutor.it,f);
	}

	default <B> Futur<B> flatMap(final Executor exec, Func1<A,Futur<B>> f) {
		final Promise<B> promise = new Promise<>();

		onComplete(exec,(e,a) -> {
			if (e == null) {
				try {
					f.call(a).onComplete(promise::set);
				} catch (Exception ex) {
					promise.set(ex, null);
				}

			} else {
				promise.set(e,null);
			}
		});

		return promise.futur;
	}

	default <B> Futur<B> then(final Executor exec, final Source1<Futur<B>> f) {
		return flatMap(exec, a -> f.call());
	}

	default <B> Futur<B> then (final Source1<Futur<B>> f) {
		return then(CurrentThreadExecutor.it, f);
	}

	default Futur<A> consume(final Executor exec, final Sink1<A> f) {
		return map(exec, a -> {
			f.call(a);
			return a;
		});
	}

	default Futur<A> consume(final Sink1<A> f) {
		return consume(CurrentThreadExecutor.it,f);
	}

	default Futur<A> onComplete(final Executor exec, final Consumer2<Exception,? super A> cb) {
		return onComplete((e,v) ->
			exec.execute(() ->
				cb.call(e,v)));
	}

	Futur<A> onComplete(Consumer2<Exception,? super A> cb);

	default Futur<A> onError(final Consumer1<Exception> cb) {
		return onComplete((e,v) -> {
			if (e != null) {
				cb.call(e);
			}
		});
	}

	default Futur<A> timeout(final ScheduledExecutorService exec, final long count, final TimeUnit unit) {
		final Promise<A> promise = new Promise<>();

		final AtomicBoolean done = new AtomicBoolean(false);

		exec.schedule(() -> {
			if (!done.getAndSet(true)) {
				promise.set(new TimeoutException(), null);
			}
		}, count, unit);

		this.onComplete((e,v) -> {
			if (!done.getAndSet(true)) {
				promise.set(e, v);
			}
		});

		return promise.futur;
	}

	default Futur<A> timeout(final ScheduledExecutorService exec, final Timeout to) {
		return timeout(exec,to.ms,TimeUnit.MILLISECONDS);
	}

	default Futur<A> deadline(final ScheduledExecutorService exec, final Deadline dl) {
		return timeout(exec, Timeout.of(dl));
	}

	static <A> Futur<A> of(final Executor exec, final Source1<A> source) {
		final Promise<A> promise = new Promise<>();

		exec.execute(() -> {
			try {
				promise.set(null, source.call());
			} catch (Exception ex) {
				promise.set(ex, null);
			}
		});

		return promise.futur;
	}

	static <A> Futur<A> of(final Source1<A> source) {
		try {
			return Futur.success(source.call());
		} catch (Exception ex) {
			return Futur.failure(ex);
		}
	}

	/////////////
	// Success //
	/////////////

	static <A> Futur<A> success(final A a) {
		return new Futur<A>() {
			@Override
			public A get(Timeout to) throws Exception {
				return a;
			}

			@Override
			public <B> Futur<B> flatMap(Func1<A, Futur<B>> f) {
				try {
					return f.call(a);
				} catch (Exception ex) {
					return failure(ex);
				}
			}

			@Override
			public Futur<A> onComplete(Consumer2<Exception,? super A> cb) {
				cb.call(null,a);
				return this;
			}

		};
	}

	/////////////
	// Failure //
	/////////////

	static <A> Futur<A> failure(final Exception e) {

		return new Futur<A>() {

			@Override
			public A get(Timeout to) throws Exception {
				throw e;
			}

			@Override
			@SuppressWarnings("unchecked")
			public <B> Futur<B> flatMap(Func1<A, Futur<B>> f) {
				return (Futur<B>) this;
			}

			@Override
			public Futur<A> onComplete(Consumer2<Exception,? super A> cb) {
				cb.call(e,null);
				return this;
			}
		};
	}

	/////////////////
	// Applicative //
	/////////////////

	static <A,B,C> Futur<C> apply2(final Futur<A> ma, final Futur<B> mb, final Func2<A,B,C> func) {
		return ma.flatMap(a ->
			mb.map(b ->
				func.call(a,b)
			)
		);
	}

	static <A,B> Futur<Pair<A,B>> zip(final Futur<A> fa, final Futur<B> fb) {
		return apply2(fa,fb,Pair::of);
	}

	///////////
	// Monad //
	///////////

	static <A> Futur<A> join(final Futur<Futur<A>> mma) {
		return mma.flatMap(ma -> ma);
	}

	static <A,B,C> Func1<A,Futur<C>> compose(final Func1<A,Futur<B>> fab, final Func1<B,Futur<C>> fbc) {
		return a -> fab.call(a).flatMap(fbc::call);
	}

}
