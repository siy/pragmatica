/*
 *  Copyright (c) 2022 Sergiy Yevtushenko.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.pragmatica.protocol.http.parser;

import org.junit.jupiter.api.Test;
import org.pragmatica.protocol.http.parser.header.StandardHttpHeaderNames;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.pragmatica.protocol.http.parser.header.StandardHttpHeaderNames.*;

class StandardHttpHeaderNamesTest {
    private static final StandardHttpHeaderNames REPEATABLE[] = {
        ACCEPT_CHARSET,
        ACCEPT_ENCODING,
        ACCEPT_LANGUAGE,
        ACCEPT,
        ALLOW,
        CACHE_CONTROL,
        CONTENT_ENCODING,
        CONTENT_LANGUAGE,
        EXPECT,
        IF_MATCH,
        IF_NONE_MATCH,
        PRAGMA,
        PROXY_AUTHENTICATE,
        PUBLIC,
        TE,
        TRAILER,
        TRANSFER_ENCODING,
        UPGRADE,
        VARY,
        VIA,
        WARNING,
        WWW_AUTHENTICATE,
        X_FORWARDED_FOR,
        ACCESS_CONTROL_ALLOW_HEADERS,
        ACCESS_CONTROL_ALLOW_METHODS,
        ACCESS_CONTROL_REQUEST_HEADERS,
        ACCESS_CONTROL_REQUEST_METHODS
    };

    @Test
    void verifyRepeatable() {
        for(var name : REPEATABLE) {
            assertTrue(name.repeatable());
        }

        var set = EnumSet.allOf(StandardHttpHeaderNames.class);
        set.removeAll(List.of(REPEATABLE));

        for(var name : set) {
            assertFalse(name.repeatable());
        }
    }
}