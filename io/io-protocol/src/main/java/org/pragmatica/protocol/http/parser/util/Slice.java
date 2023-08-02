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

package org.pragmatica.protocol.http.parser.util;

import java.nio.charset.StandardCharsets;

public interface Slice {

    static Slice fromBytes(byte[] data, int start, int end) {
        record ByteBackedSlice(byte[] data, DetachedSlice internalSlice) implements Slice {
            @Override
            public String text() {
                return internalSlice.text(data, StandardCharsets.UTF_8);
            }

            @Override
            public int len() {
                return internalSlice.len();
            }

            @Override
            public String toString() {
                return "Slice(\"" + text() + "\")";
            }
        }

        return new ByteBackedSlice(data, new DetachedSlice(start, end));
    }

    static Slice fromString(String text) {
        record StringBackedSlice(String text) implements Slice {
            @Override
            public int len() {
                return text.length();
            }

            @Override
            public String toString() {
                return "Slice(\"" + text + "\")";
            }
        }

        return new StringBackedSlice(text);
    }

    String text();

    int len();
}
