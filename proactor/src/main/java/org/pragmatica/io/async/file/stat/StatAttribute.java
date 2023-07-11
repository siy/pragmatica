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
 *
 */

package org.pragmatica.io.async.file.stat;

import java.util.EnumSet;

/**
 * File attributes.
 */
public enum StatAttribute {
    COMPRESSED(0x00000004L), /* [I] File is compressed by the fs */
    IMMUTABLE(0x00000010L),  /* [I] File is marked immutable */
    APPEND(0x00000020L),     /* [I] File is append-only */
    NODUMP(0x00000040L),     /* [I] File is not to be dumped */
    ENCRYPTED(0x00000800L),  /* [I] File requires key to decrypt in fs */
    AUTOMOUNT(0x00001000L);  /* Dir: Automount trigger */

    private final long mask;

    private long mask() {
        return mask;
    }

    StatAttribute(long mask) {
        this.mask = mask;
    }

    public static EnumSet<StatAttribute> fromLong(long attributes) {
        var result = EnumSet.noneOf(StatAttribute.class);

        for (var attribute : values()) {
            if ((attributes & attribute.mask) != 0) {
                result.add(attribute);
            }
        }
        return result;
    }

    public static long toBytes(EnumSet<StatAttribute> attributes) {
        return attributes.stream()
                         .mapToLong(StatAttribute::mask)
                         .sum();
    }
}
