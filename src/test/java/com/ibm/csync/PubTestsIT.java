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

import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class PubTestsIT {

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
                .whenComplete((result, ex) -> {
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
    public void testSimpleBlockingPub() {
        String uuid = UUID.randomUUID().toString();
        try {
            Long vts = csync.blocking.pub("tests.java."+uuid+".a.b.c", "data");
            assertTrue(vts > 0);
        }
        catch (Exception e) {
            fail("Failed to send a blocking pub");
        }
        keysToCleanup.add("tests.java."+uuid+".a.b.c");
    }

    @Test
    public void testSimpleNonBlockingPub() throws Exception {
        String uuid = UUID.randomUUID().toString();
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            csync.pub("tests.java."+uuid+".a.b.c","abc", (vts, error) -> {
                    if (error == null && vts > 0)
                        future.complete(true);
                }
            );
        }
        catch (Exception e) {
            fail("Failed to send a nonblocking pub");
        }
        assertTrue(future.get(10,TimeUnit.SECONDS));
        keysToCleanup.add("tests.java."+uuid+".a.b.c");
    }

    @Test
    public void testDeleteWildcard() throws Exception{
        String uuid = UUID.randomUUID().toString();
        Long startVTS = csync.blocking.pub("tests.java."+uuid+".a","a");
        csync.blocking.pub("tests.java."+uuid+".b","a");
        csync.blocking.pub("tests.java."+uuid+".c.c","a"); //should not be deleted
        csync.blocking.pub("tests.java."+uuid+".d","a");
        csync.blocking.pub("tests.java."+uuid+".e.e","a"); //should not be deleted
        Long finalVTS = csync.blocking.del("tests.java."+uuid+".*");
        assertTrue((finalVTS - startVTS) == 7 ); //4 more writes, and 3 should be deleted for a total of 7
        keysToCleanup.add("tests.java."+uuid+".c.c");
        keysToCleanup.add("tests.java."+uuid+".e.e");

    }

    @Test
    public void testPubWithTooManyParts() throws Exception{
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            csync.blocking.pub("tests.java.a.b.c.d.e.f.g.h.i.j.k.l.m.n.o.p.q.r.s.t.u.v.w.x.y.z", "a");
        }
        catch (Exception e){
            future.complete(true);
        }
        assertTrue(future.get(10,TimeUnit.SECONDS));
    }
}
