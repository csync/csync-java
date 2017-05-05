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

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ConnectTestsIT {

    @Test
    public void doubleLogin() throws Exception {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        final CSync csync = CSync.builder()
                .host(System.getenv("CSYNC_HOST"))
                .port(Integer.parseInt(System.getenv("CSYNC_PORT")))
                .build();
        csync.authenticate(System.getenv("CSYNC_DEMO_PROVIDER"), System.getenv("CSYNC_DEMO_TOKEN"))
                .thenCompose(result -> csync.authenticate(System.getenv("CSYNC_DEMO_PROVIDER"), System.getenv("CSYNC_DEMO_TOKEN")))
                .thenRun(() -> future.complete(true))
                .exceptionally((ex) -> {
                    fail("Test failed, should be able to authenticate twice");
                    return null;
                });
        assertTrue(future.get(10, TimeUnit.SECONDS));
    }

    @Test
    public void unauthenticateFirst() throws Exception {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        final CSync csync = CSync.builder()
                .host(System.getenv("CSYNC_HOST"))
                .port(Integer.parseInt(System.getenv("CSYNC_PORT")))
                .build();
        csync.unauthenticate()
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        future.complete(true);
                    } else {
                        fail("Test failed, unable to unauthenticate without being authenticated");
                    }
                });
        assertTrue(future.get(10, TimeUnit.SECONDS));
    }

    @Test
    public void authOnceUnauthTwice() throws Exception {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        final CSync csync = CSync.builder()
                .host(System.getenv("CSYNC_HOST"))
                .port(Integer.parseInt(System.getenv("CSYNC_PORT")))
                .build();
        csync.authenticate(System.getenv("CSYNC_DEMO_PROVIDER"), System.getenv("CSYNC_DEMO_TOKEN"))
                .thenCompose(result -> csync.unauthenticate())
                .thenCompose(result -> csync.unauthenticate())
                .thenRun(() -> future.complete(true))
                .exceptionally(ex -> {
                    fail("Test failed, unable to unauthenticate after unauthenticating");
                    return null;
                });
        assertTrue(future.get(10, TimeUnit.SECONDS));
    }

    @Test
    public void unauthorizedPub() throws Exception {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        final CSync csync = CSync.builder()
                .host(System.getenv("CSYNC_HOST"))
                .port(Integer.parseInt(System.getenv("CSYNC_PORT")))
                .build();
        try {
            Long vts = csync.blocking.pub("tests.java.a.b.c", "data");
            fail("Should not have been able to pub without being authorized");
        } catch (Exception exception) {
            future.complete(true);
        }
        assertTrue(future.get(10, TimeUnit.SECONDS));
    }

    @Test
    public void actionAfterMultipleAuthChanges() throws Exception {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        final CSync csync = CSync.builder()
                .host(System.getenv("CSYNC_HOST"))
                .port(Integer.parseInt(System.getenv("CSYNC_PORT")))
                .build();
        csync.authenticate(System.getenv("CSYNC_DEMO_PROVIDER"), System.getenv("CSYNC_DEMO_TOKEN"))
                .thenCompose(result -> csync.unauthenticate())
                .thenCompose(result -> csync.authenticate(System.getenv("CSYNC_DEMO_PROVIDER"), System.getenv("CSYNC_DEMO_TOKEN")))
                .thenRun(() -> {
                    String uuid = UUID.randomUUID().toString();
                    try {
                        Long vts = csync.blocking.pub("tests.java." + uuid + ".a", "b");
                        future.complete(vts > 0);
                        //Clean up key after test
                        csync.blocking.del("tests.java." + uuid + ".a");
                    } catch (Exception exception) {
                        fail("Test failed, error pub or deleting " + exception.getLocalizedMessage());
                    }
                })
                .exceptionally(ex -> {
                    fail("Test failed, unable to authenticate after unauthenticating");
                    return null;
                });
        assertTrue(future.get(10, TimeUnit.SECONDS));
    }

    @Test
    public void multipleSimultaneousLogin() throws Exception {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        final CSync csync = CSync.builder()
                .host(System.getenv("CSYNC_HOST"))
                .port(Integer.parseInt(System.getenv("CSYNC_PORT")))
                .build();
        csync.authenticate(System.getenv("CSYNC_DEMO_PROVIDER"), System.getenv("CSYNC_DEMO_TOKEN"));
        csync.authenticate(System.getenv("CSYNC_DEMO_PROVIDER"), System.getenv("CSYNC_DEMO_TOKEN"))
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        future.complete(true);
                    } else {
                        fail("Test failed, unable to log in with second login attempt");
                    }
                });
        assertTrue(future.get(10, TimeUnit.SECONDS));
    }

    @Test
    public void multipleSimultaneousLogout() throws Exception {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        final CSync csync = CSync.builder()
                .host(System.getenv("CSYNC_HOST"))
                .port(Integer.parseInt(System.getenv("CSYNC_PORT")))
                .build();
        csync.authenticate(System.getenv("CSYNC_DEMO_PROVIDER"), System.getenv("CSYNC_DEMO_TOKEN"))
                .thenCompose(result -> {
                    csync.unauthenticate();
                    return csync.unauthenticate();
                })
                .thenRun(() -> future.complete(true))
                .exceptionally((ex) -> {
                    fail("Test failed, unable to unauthenticate during simultaneous unauthenticating");
                    return null;
                });
        assertTrue(future.get(10, TimeUnit.SECONDS));
    }

    @Test
    public void authUnauthAction() throws Exception {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        final CSync csync = CSync.builder()
                .host(System.getenv("CSYNC_HOST"))
                .port(Integer.parseInt(System.getenv("CSYNC_PORT")))
                .build();
        csync.authenticate(System.getenv("CSYNC_DEMO_PROVIDER"), System.getenv("CSYNC_DEMO_TOKEN"))
                .thenCompose((result) -> csync.unauthenticate())
                .thenRun(() -> {
                    try {
                        String uuid = UUID.randomUUID().toString();
                        Long vts = csync.blocking.pub("tests.java." + uuid + ".a", "b");
                        fail("Should not have been able to complete pub when logged out");
                        //Clean up key after test
                        csync.blocking.del("tests.java." + uuid + ".a");
                    } catch (Exception exception) {
                        future.complete(true);
                    }
                })
                .exceptionally((ex) -> {
                    fail("Test failed, unable to unauthenticate after authenticating");
                    return null;
                });
        assertTrue(future.get(10, TimeUnit.SECONDS));
    }

    @Test
    public void guestLogin() throws Exception {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        final CSync csync = CSync.builder()
                .host(System.getenv("CSYNC_HOST"))
                .port(Integer.parseInt(System.getenv("CSYNC_PORT")))
                .build();
        csync.authenticate(System.getenv("CSYNC_DEMO_PROVIDER"), System.getenv("CSYNC_DEMO_TOKEN"))
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        future.complete(true);
                    } else {
                        fail("Test failed, unable to log in with demo token");
                    }
                });
        assertTrue(future.get(10, TimeUnit.SECONDS));
    }

    @Test
    public void badProvider() throws Exception {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        final CSync csync = CSync.builder()
                .host(System.getenv("CSYNC_HOST"))
                .port(Integer.parseInt(System.getenv("CSYNC_PORT")))
                .build();
        csync.authenticate("thisisafakeprovider", System.getenv("CSYNC_DEMO_TOKEN"))
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        future.complete(true);
                    } else {
                        fail("should not have been able to login with bad provider");
                    }
                });

        assertTrue(future.get(10, TimeUnit.SECONDS));
    }

    @Test
    public void badGuestLogin() throws Exception {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        final CSync csync = CSync.builder()
                .host(System.getenv("CSYNC_HOST"))
                .port(Integer.parseInt(System.getenv("CSYNC_PORT")))
                .build();
        csync.authenticate(System.getenv("CSYNC_DEMO_PROVIDER"), "thistokenshouldfail")
                .thenRun(() -> fail("should not have been able to do login with a bad login"))
                .exceptionally(ex -> {
                    future.complete(true);
                    return null;
                });
        assertTrue(future.get(10, TimeUnit.SECONDS));
    }

    @Test
    public void badFacebookLogin() throws Exception {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        final CSync csync = CSync.builder()
                .host(System.getenv("CSYNC_HOST"))
                .port(Integer.parseInt(System.getenv("CSYNC_PORT")))
                .build();

        csync.authenticate("facebook", "thisisafaketoken")
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        future.complete(true);
                    } else {
                        fail("should not have been able to do login with a bad login");
                    }
                });
        assertTrue(future.get(10, TimeUnit.SECONDS));
    }

    @Test
    public void badGoogleLogin() throws Exception {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        final CSync csync = CSync.builder()
                .host(System.getenv("CSYNC_HOST"))
                .port(Integer.parseInt(System.getenv("CSYNC_PORT")))
                .build();

        csync.authenticate("google", "thisisafaketoken")
                .whenComplete((isSuccessful, ex) -> {
                    if (ex != null) {
                        future.complete(true);
                    } else {
                        fail("should not have been able to do login with a bad login");
                    }
                });
        assertTrue(future.get(10, TimeUnit.SECONDS));
    }

    @Test
    public void raceConditionOnLogin() throws Exception {
        String uuid = UUID.randomUUID().toString();
        final CSync csync = CSync.builder()
                .host(System.getenv("CSYNC_HOST"))
                .port(Integer.parseInt(System.getenv("CSYNC_PORT")))
                .build();
        csync.authenticate(System.getenv("CSYNC_DEMO_PROVIDER"), System.getenv("CSYNC_DEMO_TOKEN"));
        //Try to pub before authenticate returns, it should eventually succeed
        Long vts = csync.blocking.pub("tests.java." + uuid + ".a", "b");
        assertTrue(vts > 0);

        //Clean up key after test
        csync.blocking.del("tests.java." + uuid + ".a");
    }
}
