/*
 *  Copyright (c) 2020-2022 Sergiy Yevtushenko.
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
 */

package org.pragmatica.io.async.util.raw;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RawMemoryTest {
    @Test
    void instanceCanBeObtained() {
        var address = RawMemory.allocate(1024);
        RawMemory.dispose(address);

        assertTrue(address != 0);
    }

    @Test
    void rewMemoryCanBeWrittenAndRead() {
        var address = RawMemory.allocate(1024);

        try {
            RawMemory.putLong(address, 0xCAFEBABEL);

            var value = RawMemory.getLong(address);

            assertEquals(0xCAFEBABEL, value);
        } finally {
            RawMemory.dispose(address);
        }


        assertTrue(address != 0);
    }
}