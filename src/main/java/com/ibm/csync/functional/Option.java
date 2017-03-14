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

public interface Option<A> {

	boolean isEmpty();
	A get() throws IllegalStateException;
	void forEach(final Sink1<A> sink) throws Exception;
	<B> Option<B> map(final Func1<A,B> f) throws Exception;
	<B> Option<B> flatMap(final Func1<A,Option<B>> f) throws Exception;
	A getOrElse(Source1<A> f) throws Exception;

	Option<Object> none_ = new Option<Object>() {
		@Override
		public boolean isEmpty() {
			return true;
		}

		@Override
		public Object get() throws IllegalStateException {
			throw new IllegalStateException();
		}

		@Override
		public void forEach(Sink1<Object> f) throws Exception {
		}


		@Override
		@SuppressWarnings("unchecked")
		public <B> Option<B> map(Func1<Object, B> f) {
			return (Option<B>)this;
		}

		@Override
		@SuppressWarnings("unchecked")
		public <B> Option<B> flatMap(Func1<Object, Option<B>> f) {
			return (Option<B>)this;
		}

		@Override
		public Object getOrElse(Source1<Object> f) throws Exception {
			return f.call();
		}
	};

	@SuppressWarnings("unchecked")
	static <B> Option<B> none() {
		return (Option<B>)none_;
	}

	static <T> Option<T> some(final T value) {
		return new Option<T>() {
			@Override
			public boolean isEmpty() {
				return false;
			}

			@Override
			public T get() throws IllegalStateException {
				return value;
			}

			@Override
			public void forEach(Sink1<T> f) throws Exception {
				f.call(value);
			}

			@Override
			public <B> Option<B> map(Func1<T, B> f) throws Exception {
				return some(f.call(value));
			}

			@Override
			public <B> Option<B> flatMap(Func1<T, Option<B>> f) throws Exception {
				return f.call(value);
			}

			@Override
			public T getOrElse(Source1<T> f) throws Exception {
				return value;
			}
		};

	}
}
