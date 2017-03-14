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

package com.ibm.csync.impl;

import com.google.gson.JsonElement;

import java.util.concurrent.atomic.AtomicLong;

public class Envelope {

    private static final AtomicLong nextId = new AtomicLong(0);

    String kind;
    public JsonElement payload;
    Long version;
    Long closure;

    Envelope(String kind, JsonElement payload) {
        this.kind = kind;
        this.payload = payload;
        this.version = 15L;
        this.closure = nextId.getAndIncrement();
    }

    @Override public String toString() {
        return String.format("{kind:%s payload=%s}",kind,payload.toString());
    }
}
