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

package org.pragmatica.io.async.file.stat;

/**
 * File descriptor types.
 */
public enum FileType {
    REGULAR(0100000),   /* Regular file.  */
    DIRECTORY(0040000), /* Directory.  */
    CHARACTER(0020000), /* Character device.  */
    BLOCK(0060000),     /* Block device.  */
    FIFO(0010000),      /* FIFO.  */
    LINK(0120000),      /* Symbolic link.  */
    SOCKET(0140000),    /* Socket.  */;

    private final short mask;

    FileType(final int mask) {
        this.mask = (short) mask;
    }

    public short mask() {
        return mask;
    }

    public static FileType unsafeFromShort(final short value) {
        for (var type : values()) {
            if ((value & type.mask) == type.mask) {
                return type;
            }
        }
        return null;
    }
}
