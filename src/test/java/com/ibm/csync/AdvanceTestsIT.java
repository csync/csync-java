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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AdvanceTestsIT {

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
                .onComplete((ex, isSuccessful) -> {
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

    int totalCount = 0 ;

    @Test
    public void testSimpleAdvance() throws Exception{
        String uuid = UUID.randomUUID().toString();


        CompletableFuture<Boolean> future = new CompletableFuture<>();
        csync.blocking.pub("tests.java." + uuid + ".a",String.valueOf(0));
        csync.blocking.pub("tests.java." + uuid + ".a.b",String.valueOf(0));
        csync.blocking.pub("tests.java." + uuid + ".a.b.c",String.valueOf(0));
        csync.blocking.pub("tests.java." + uuid + ".a.b.c.d",String.valueOf(0));
        Closeable listener = csync.listen(
                Key.of("tests.java."+uuid+".#"),
                data -> {
                    totalCount++;
                    if (totalCount == 200){
                        future.complete(true);
                    }

                    int value = Integer.parseInt(data.data);
                    if(value < 50){
                        csync.blocking.pub(data.key.string,String.valueOf(value+1));
                    }
                });
        assertTrue(future.get(30, TimeUnit.SECONDS));
        listener.close();
        keysToCleanup.add("tests.java." + uuid + ".a");
        keysToCleanup.add("tests.java." + uuid + ".a.b");
        keysToCleanup.add("tests.java." + uuid + ".a.b.c");
        keysToCleanup.add("tests.java." + uuid + ".a.b.c.d");
    }
}
