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

import com.ibm.csync.Acls;
import com.ibm.csync.CSync;
import com.ibm.csync.Callback;
import com.ibm.csync.Key;
import com.ibm.csync.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.Date;
import java.util.concurrent.*;

public class Main {

	static private final Logger logger = LoggerFactory.getLogger(Main.class);

	private static Callback<Object> print(final String msg) {
		return (e, v) -> logger.info("msg:[{}] err:[{}] result:[{}]", msg, e, v);
	}

	public static void main(String args[]) throws Exception {

		//final Callback<Object> print = (e,v) -> logger.info("[{}] [{}]",e,v);


		final CSync csync = CSync.builder()
			.token("demoToken")
			.provider("demo")
			.build();

		csync.pub("something", "hello", Acls.Private)
			.then(() -> csync.pub("something", "hello again", Acls.PublicRead))
			.onComplete(print("something public"));

		final Closeable all = csync
			.listen(
				Key.of("#"),
				data -> logger.info("[all] {}", data));

		logger.info("pub vts {}", csync.blocking.pub("x.y.z", "nice"));

		csync.del("x.y.z", Timeout.of(10000), print("delete x.y.z"));
		csync.pub(Key.of("a", "b"), "xyz");

		csync.pub("nice.key", "hello")
			.then(() -> csync.del("nice.key"))
			.onComplete(print("pub nice.key:hello"));

		final CountDownLatch waitForThree = new CountDownLatch(3);
		csync.del()
		csync.blocking.pub("a","a",Timeout.of(10000));
		final Closeable s1 = csync.listen(
			Key.of("#"),
			data -> {
				//logger.info("s1 {}", data);
				if (data.isDeleted) {
					waitForThree.countDown();
				} else {
					if (!"something".equals(data.key.array[0])) csync.del(data.key);
				}
			}
		);

		csync.listen(
			Key.of("x.#"),
			data -> {
				//logger.info("s2 {}", data);
				if (data.isDeleted) {
					waitForThree.countDown();
				}
			}
		);

		logger.info("waiting for 3 deleted items");
		waitForThree.await();
		logger.info("back");

		s1.close();

		csync.listen(Key.of("a.b.*"), data -> {
			logger.info("s3 {}", data);
			if (!data.isDeleted) {
				csync.del("a.b.c");
			}
		});

		csync
			.pub("a.b.c", "abc")
			.onComplete(print("pub a.b.c"));

		// Callback API

		final Key pqr = Key.of("p.q.r");

		csync.pub(pqr, "pqr", (e, v) ->
			csync.pub(pqr, "xyz", (e1, v1) ->
				csync.del(pqr, (e2, v2) ->
					logger.info("callback {} {} {}", v, v1, v2)
				)
			)
		);

		// Blocking API

		final Key lmn = Key.of("l.m.n");

		Long b1 = csync.blocking.pub(lmn, "lmn");
		Long b2 = csync.blocking.pub(lmn, "lmn2");
		Long b3 = csync.blocking.del(lmn);

		logger.info("blocking {} {} {}", b1, b2, b3);

		// loop

		final Runnable loop = () -> {
			try {
				logger.info("pub");
				csync
					.pub("done.in.loop", new Date().toString())
					.onComplete(print("loop"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		};
		all.close();
		csync.workers().scheduleAtFixedRate(loop, 0, 1, TimeUnit.SECONDS);
	}
}
