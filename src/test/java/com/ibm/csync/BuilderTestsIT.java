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
import static org.junit.Assert.*;

public class BuilderTestsIT {

    @Test(expected=IllegalArgumentException.class)
    public void testNullTimeout() throws Exception{
        CSync csync = CSync.builder()
                .defaultBlockingTimeout(null)
                .build();
        fail("Was able to build with no timeout");
    }

   @Test(expected=IllegalArgumentException.class)
    public void testNullWorkers() throws Exception{
        CSync csync = CSync.builder()
                .workers(null)
                .build();
        fail("Was able to build with null workers");
    }

  @Test(expected=IllegalArgumentException.class)
    public void testBadWorkers() throws Exception{
        CSync csync = CSync.builder()
                .defaultBlockingTimeout(Timeout.of(-5))
                .build();
        fail("Was able to build with negative timeout");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testBadPort() throws Exception{
        CSync csync = CSync.builder()
                .port(-5)
                .build();
        fail("Was able to build with negative port");
    }


}
