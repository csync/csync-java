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

import java.util.concurrent.Callable;
import java.util.function.Function;

public interface Try<T> {

	boolean isHappy();
	T get() throws Exception;
	<B> Try<B> flatMap(final Function<T,Try<B>> f);
	<B> Try<B> map(final Function<T,B> f);




	static <A> Try<A> success(final A value) {
		return new Try<A>() {
			@Override
			public boolean isHappy() {
				return true;
			}

			@Override
			public A get() throws Exception {
				return value;
			}

			@Override
			public <B> Try<B> flatMap(Function<A, Try<B>> f) {
				try {
					return f.apply(value);
				} catch (Throwable th) {
					return failure(rethrowIfFatal(th));
				}
			}

			@Override
			public <B> Try<B> map(Function<A, B> f) {
				try {
					return success(f.apply(value));
				} catch (Throwable th) {
					return failure(rethrowIfFatal(th));
				}
			}


		};
	}

	static <A> Try<A> failure(final Exception ex) {
		return new Try<A>() {
			@Override
			public boolean isHappy() {
				return false;
			}

			@Override
			public A get() throws Exception {
				throw ex;
			}

			@Override
			public <B> Try<B> flatMap(Function<A, Try<B>> f) {
				return failure(ex);
			}

			@Override
			public <B> Try<B> map(Function<A, B> f) {
				return failure(ex);
			}
		};
	}

	// TODO: handle more panics
	static Exception rethrowIfFatal(Throwable t) {
		try {
			throw t;
		} catch (Exception ex) {
			return ex;
		} catch (Error err) {
			throw err;
		} catch (Throwable th) {
			return new Exception(th);
		}
	}

	static <T> Try<T> of(final Callable<T> f) {
		try {
			return success(f.call());
		} catch (Throwable t) {
			return failure(rethrowIfFatal(t));
		}
	}



}
