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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

public class Promise<A> {

	private A a = null;
	private Exception ex = null;
	private boolean done = false;
	private List<Consumer2<Exception,? super A>> waiting = null;
	private final Executor home;

	public Promise(final Executor home) {
		this.home = home;
	}

	public Promise() {
		this(CurrentThreadExecutor.it);
	}

	public void set(final Executor on, final Exception ex, final A a) {
		final List<Consumer2<Exception,? super A>> wakeup;

		synchronized (this) {
			if (done) throw new IllegalStateException("already set");
			this.a = a;
			this.ex = ex;
			this.done = true;
			wakeup = waiting;
			waiting = null;
			notifyAll();
		}

		on.execute(() -> {
			if (wakeup != null) {
				for (final Consumer2<Exception, ? super A> cb : wakeup) {
					cb.call(ex, a);
				}
			}
		});
	}

	public void set(final Exception ex, final A a) {
		set(home, ex, a);
	}

	public final Futur<A> futur = new Futur<A>() {

		@Override
		public  Futur<A> onComplete(Consumer2<Exception,? super A> cb) {
			if (done) {
				cb.call(ex, a);
			} else {
				synchronized (Promise.this) {
					if (waiting == null) waiting = new ArrayList<>();
					waiting.add(cb);
				}
			}
			return this;
		}
	};

	public static <T> void set(final Promise<T> promise, final Exception ex, final T value) {
		if (promise != null) {
			promise.set(ex,value);
		}
	}

}
