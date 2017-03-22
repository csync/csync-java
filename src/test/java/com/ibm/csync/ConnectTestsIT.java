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

import org.junit.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ConnectTestsIT {

    @Test
    public void guestLogin() {
        try {
            final CSync csync = CSync.builder()
                    .token(System.getenv("CSYNC_DEMO_TOKEN"))
                    .provider(System.getenv("CSYNC_DEMO_PROVIDER"))
                    .host(System.getenv("CSYNC_HOST"))
                    .port(Integer.parseInt(System.getenv("CSYNC_PORT")))
                    .build();

            csync.blocking.pub("a","a");

        }
        catch (Exception e){
            fail("Test failed, unable to log in with demo token");
        }
    }

    @Test
    public void badProvider() throws Exception{
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            final CSync csync = CSync.builder()
                    .token(System.getenv("CSYNC_DEMO_TOKEN"))
                    .provider("thisisafakeprovider")
                    .host(System.getenv("CSYNC_HOST"))
                    .port(Integer.parseInt(System.getenv("CSYNC_PORT")))
                    .build();

            csync.blocking.pub("a","a");
            fail("should have been able to do a blocking pub with a bad login");

        }
        catch (Exception e){
            future.complete(true);
        }
        assertTrue(future.get(10, TimeUnit.SECONDS));
    }

    @Test
    public void badGuestLogin() throws Exception{
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            final CSync csync = CSync.builder()
                    .token("thistokenshouldfail")
                    .provider(System.getenv("CSYNC_DEMO_PROVIDER"))
                    .host(System.getenv("CSYNC_HOST"))
                    .port(Integer.parseInt(System.getenv("CSYNC_PORT")))
                    .build();

            csync.blocking.pub("a","a");
            fail("should have been able to do a blocking pub with a bad login");

        }
        catch (Exception e){
            future.complete(true);
        }
        assertTrue(future.get(10, TimeUnit.SECONDS));
    }

    @Test
    public void badFacebookLogin() throws Exception{
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            final CSync csync = CSync.builder()
                    .token("thisisafaketoken")
                    .provider("facebook")
                    .host(System.getenv("CSYNC_HOST"))
                    .port(Integer.parseInt(System.getenv("CSYNC_PORT")))
                    .build();

            csync.blocking.pub("a","a");
            fail("should have been able to do a blocking pub with a bad login");

        }
        catch (Exception e){
            future.complete(true);
        }
        assertTrue(future.get(10, TimeUnit.SECONDS));
    }

    @Test
    public void badGoogleLogin() throws Exception{
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        try {
            final CSync csync = CSync.builder()
                    .token("thisisafaketoken")
                    .provider("google")
                    .host(System.getenv("CSYNC_HOST"))
                    .port(Integer.parseInt(System.getenv("CSYNC_PORT")))
                    .build();

            csync.blocking.pub("a","a");
            fail("should have been able to do a blocking pub with a bad login");

        }
        catch (Exception e){
            future.complete(true);
        }
        assertTrue(future.get(10, TimeUnit.SECONDS));
    }
}
