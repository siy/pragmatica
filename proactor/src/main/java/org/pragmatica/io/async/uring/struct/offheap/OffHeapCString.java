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

package org.pragmatica.io.async.uring.struct.offheap;

import org.pragmatica.io.async.util.raw.RawMemory;

import java.nio.charset.StandardCharsets;

/**
 * C-style string container. At the native code side strings are accessible as UTF-8 encoded, zero-terminated byte arrays.
 */
public class OffHeapCString extends AbstractOffHeapStructure<OffHeapCString> {
    private OffHeapCString(final byte[] input) {
        super(input.length + 1);
        clear();
        RawMemory.putByteArray(address(), input);
    }

    public static OffHeapCString cstring(final String string) {
        return new OffHeapCString(string.getBytes(StandardCharsets.UTF_8));
    }
}
