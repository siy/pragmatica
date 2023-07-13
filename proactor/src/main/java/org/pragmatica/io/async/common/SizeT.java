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

package org.pragmatica.io.async.common;

/**
 * Representation of the various values meaning 'size' of the something, for example size of read/writen chunk of data.
 */
public record SizeT (long value) implements Comparable<SizeT> {
    public static final SizeT ZERO = sizeT(0L);

    public static SizeT sizeT(final long value) {
        return new SizeT(value);
    }

    @Override
    public String toString() {
        return "SizeT(" + value + ")";
    }

    @Override
    public int compareTo(SizeT o) {
        return Long.compare(value, o.value);
    }
}
