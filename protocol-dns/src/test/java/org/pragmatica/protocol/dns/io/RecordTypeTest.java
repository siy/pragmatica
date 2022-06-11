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

package org.pragmatica.protocol.dns.io;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.pragmatica.protocol.dns.io.RecordType.*;

class RecordTypeTest {
    @Test
    void checkResourceFlag() {
        var notResource = List.of(OPT, TKEY, TSIG, IXFR, AXFR, MAILB, MAILA, ANY);

        notResource.forEach(value -> assertFalse(value.resource()));

        var resources = EnumSet.allOf(RecordType.class);
        resources.removeAll(notResource);

        resources.forEach(value -> assertTrue(value.resource()));
    }
}