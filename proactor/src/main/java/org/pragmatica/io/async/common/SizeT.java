/*
 * Copyright (c) 2020 Sergiy Yevtushenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pragmatica.io.async.common;

/**
 * Representation of the various values meaning 'size' of the something, for example size of read/writen chunk of data.
 */
public class SizeT implements Comparable<SizeT> {
    private final long value;

    private SizeT(final long value) {
        this.value = value;
    }

    public static final SizeT ZERO = sizeT(0L);

    public static SizeT sizeT(final long value) {
        return new SizeT(value);
    }

    public long value() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        } else if (o instanceof SizeT sizeT) {
            return value == sizeT.value;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return (int) (value ^ (value >>> 32));
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
