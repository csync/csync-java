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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class SubTestsIT {

    CSync csync;
    ArrayList<String> keysToCleanup = new ArrayList<String>();

    @Before
    public void setup() throws Exception{
        int x = 50;
        csync = CSync.builder()
                .token(System.getenv("CSYNC_DEMO_TOKEN"))
                .provider(System.getenv("CSYNC_DEMO_PROVIDER"))
                .host(System.getenv("CSYNC_HOST"))
                .port(Integer.parseInt(System.getenv("CSYNC_PORT")))
                .build();
    }

    @After
    public void teardown(){
        for(int i = 0; i < keysToCleanup.size(); i++){
            try {
                csync.blocking.del(keysToCleanup.get(i));
            }
            catch(Exception e){}
        }
        keysToCleanup.clear();
        csync = null;
    }

    @Test
    public void testBasicListen() throws Exception{
        String uuid = UUID.randomUUID().toString();
        CompletableFuture<String> future = new CompletableFuture<>();
        Closeable listener = csync.listen(
                Key.of("tests.java."+uuid+".*"),
                data -> {
                    assertTrue(data.isDeleted == false);
                    assertTrue(data.data.equals("abc"));
                    assertTrue(data.vts > 0);
                    assertTrue(data.cts > 0);
                    assertTrue(data.acl.id().equals("$publicCreate"));
                    future.complete("pass");
                });
        csync.pub("tests.java."+uuid+".a","abc");
        assertTrue(future.get(20, TimeUnit.SECONDS).equals("pass"));
        listener.close(); //close so we don't get events for teardown
        keysToCleanup.add("tests.java."+uuid+".*");
    }

    @Test
    public void testMultipleListeners() throws Exception {
        String uuid = UUID.randomUUID().toString();
        CompletableFuture<String> futureOne = new CompletableFuture<>();
        CompletableFuture<String> futureTwo = new CompletableFuture<>();
        Closeable listenerOne = csync.listen(
                Key.of("tests.java."+uuid+".*"),
                data -> {
                    assertTrue(data.isDeleted == false);
                    assertTrue(data.data.equals("abc"));
                    assertTrue(data.vts>0);
                    assertTrue(data.cts>0);
                    assertTrue(data.acl.id().equals("$publicCreate"));
                    futureOne.complete("pass");
                });

        Closeable listener = csync.listen(
                Key.of("tests.java."+uuid+".a"),
                data -> {
                    assertTrue(data.isDeleted == false);
                    assertTrue(data.data.equals("abc"));
                    assertTrue(data.vts>0);
                    assertTrue(data.cts>0);
                    assertTrue(data.acl.id().equals("$publicCreate"));
                    futureTwo.complete("pass");
                });

        csync.pub("tests.java."+uuid+".a","abc");
        assertTrue(futureOne.get(20, TimeUnit.SECONDS).equals("pass"));
        assertTrue(futureTwo.get(20, TimeUnit.SECONDS).equals("pass"));
        listenerOne.close(); //close so we don't get events for teardown
        listener.close();
        keysToCleanup.add("tests.java." + uuid + ".a");
    }

}
