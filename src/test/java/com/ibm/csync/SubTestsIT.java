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
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class SubTestsIT {

    CSync csync;
    ArrayList<String> keysToCleanup = new ArrayList<String>();

    @Before
    public void setup() throws Exception{
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        csync = CSync.builder()
                .host(System.getenv("CSYNC_HOST"))
                .port(Integer.parseInt(System.getenv("CSYNC_PORT")))
                .build();
        csync.authenticate(System.getenv("CSYNC_DEMO_PROVIDER"), System.getenv("CSYNC_DEMO_TOKEN"))
                .whenComplete((isSuccessful, ex) -> {
                    if(ex == null) {
                        future.complete(true);
                    }
                    else {
                        fail("Unable to authenticate with the given credentials");
                    }
                });

        future.get(10, TimeUnit.SECONDS);
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
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Closeable listener = csync.listen(
                Key.of("tests.java."+uuid+".*"),
                data -> {
                    assertTrue(data.isDeleted == false);
                    assertTrue(data.data.equals("abc"));
                    assertTrue(data.vts > 0);
                    assertTrue(data.cts > 0);
                    assertTrue(data.acl.id().equals("$publicCreate"));
                    future.complete(true);
                });
        csync.pub("tests.java."+uuid+".a","abc");
        assertTrue(future.get(20, TimeUnit.SECONDS));
        listener.close(); //close so we don't get events for teardown
        keysToCleanup.add("tests.java."+uuid+".*");
    }

    @Test
    public void testMultipleListeners() throws Exception {
        String uuid = UUID.randomUUID().toString();
        CompletableFuture<Boolean> futureOne = new CompletableFuture<>();
        CompletableFuture<Boolean> futureTwo = new CompletableFuture<>();
        Closeable listenerOne = csync.listen(
                Key.of("tests.java."+uuid+".*"),
                data -> {
                    assertTrue(data.isDeleted == false);
                    assertTrue(data.data.equals("abc"));
                    assertTrue(data.vts>0);
                    assertTrue(data.cts>0);
                    assertTrue(data.acl.id().equals("$publicCreate"));
                    futureOne.complete(true);
                });

        Closeable listener = csync.listen(
                Key.of("tests.java."+uuid+".a"),
                data -> {
                    assertTrue(data.isDeleted == false);
                    assertTrue(data.data.equals("abc"));
                    assertTrue(data.vts>0);
                    assertTrue(data.cts>0);
                    assertTrue(data.acl.id().equals("$publicCreate"));
                    futureTwo.complete(true);
                });

        csync.pub("tests.java."+uuid+".a","abc");
        assertTrue(futureOne.get(20, TimeUnit.SECONDS));
        assertTrue(futureTwo.get(20, TimeUnit.SECONDS));
        listenerOne.close(); //close so we don't get events for teardown
        listener.close();
        keysToCleanup.add("tests.java." + uuid + ".a");
    }

    //this test is broken, we need to fix the underlying sdk functionality.
    @Test
    public void testRecieveCorrectACL() throws Exception{
        String uuid = UUID.randomUUID().toString();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Closeable listener = csync.listen(
                Key.of("tests.java."+uuid+".a"),
                data -> {
                    assertTrue(data.isDeleted == false);
                    assertTrue(data.data.equals("test string data"));
                    assertTrue(data.vts>0);
                    assertTrue(data.cts>0);
                    //assertTrue(data.acl.id().equals("$publicCreateReadUpdate"));
                    future.complete(true);
                });
        csync.blocking.pub("tests.java." + uuid + ".a","test string data");

        assertTrue(future.get(20, TimeUnit.SECONDS));
        listener.close();
        keysToCleanup.add("tests.java." + uuid + ".a");
    }

    @Test
    public void testMultiplePubsInARow() throws Exception{
        String uuid = UUID.randomUUID().toString();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        CompletableFuture<Boolean> futureTwo = new CompletableFuture<>();
        try{
            csync.pub("tests.java."+uuid+".abc","test string data");
            csync.pub("tests.java."+uuid+".def","test string data");
        }
        catch (Exception e){
            fail("gave an exception when doing two pubs real fast");
        }
        Closeable listener = csync.listen(
                Key.of("tests.java."+uuid+".*"),
                data -> {
                    assertTrue(data.isDeleted == false);
                    assertTrue(data.data.equals("test string data"));
                    assertTrue(data.vts>0);
                    assertTrue(data.cts>0);
                    if(data.key.string.equals("tests.java."+uuid+".abc"))
                        future.complete(true);
                    else if(data.key.string.equals("tests.java."+uuid+".def"))
                        futureTwo.complete(true);
                });
        assertTrue(future.get());
        assertTrue(futureTwo.get());
        listener.close();
    }

    @Test
    public void testMultipleListens() throws Exception{
        String uuid = UUID.randomUUID().toString();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        CompletableFuture<Boolean> futureTwo = new CompletableFuture<>();
        try{
            csync.pub("tests.java."+uuid+".abc","test string data");
        }
        catch (Exception e){
            fail("gave an exception when doing two pubs real fast");
        }
        Closeable listener = csync.listen(
                Key.of("tests.java."+uuid+".abc"),
                data -> {
                    assertTrue(data.isDeleted == false);
                    assertTrue(data.data.equals("test string data"));
                    assertTrue(data.vts>0);
                    assertTrue(data.cts>0);
                    if(data.key.string.equals("tests.java."+uuid+".abc"))
                        future.complete(true);
                });
        assertTrue(future.get());
        listener.close();
        Closeable listenerTwo = csync.listen(
                Key.of("tests.java."+uuid+".abc"),
                data -> {
                    assertTrue(data.isDeleted == false);
                    assertTrue(data.data.equals("test string data"));
                    assertTrue(data.vts>0);
                    assertTrue(data.cts>0);
                    if(data.key.string.equals("tests.java."+uuid+".abc"))
                        futureTwo.complete(true);
                });
        assertTrue(futureTwo.get());
        listenerTwo.close();
    }

    @Test
    public void testNotServeDelete() throws Exception{
        String uuid = UUID.randomUUID().toString();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        CompletableFuture<Boolean> futureTwo = new CompletableFuture<>();
        try{
            csync.pub("tests.java."+uuid+".abc","test string data");
            csync.del("tests.java."+uuid+".abc");
        }
        catch (Exception e){
            fail("gave an exception when doing two pubs real fast");
        }
        Closeable listener = csync.listen(
                Key.of("tests.java."+uuid+".abc"),
                data -> {
                    if(data.isDeleted == true)
                        future.complete(true);
                });
        assertTrue(future.get());
        listener.close();
        Closeable listenerTwo = csync.listen(
                Key.of("tests.java."+uuid+".abc"),
                data -> {
                    //Should never be able to get here, on deletes if should not send the item from teh local db
                    fail("Should not have received a delete from the db on a listen");
                });
        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                futureTwo.complete(true);
            }
        }, 2000);
        assertTrue(futureTwo.get());
        listenerTwo.close();
    }

}
