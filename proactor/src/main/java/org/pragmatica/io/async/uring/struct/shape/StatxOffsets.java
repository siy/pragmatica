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

package org.pragmatica.io.async.uring.struct.shape;

import org.pragmatica.io.async.util.raw.RawProperty;

/**
 * Offsets/Sizes of the fields of the {@link org.pragmatica.io.async.uring.struct.raw.RawStatx}
 */
public interface StatxOffsets {
    int SIZE = 256;
    RawProperty stx_mask = RawProperty.raw(0, 4);
    RawProperty stx_blksize = RawProperty.raw(4, 4);
    RawProperty stx_attributes = RawProperty.raw(8, 8);
    RawProperty stx_nlink = RawProperty.raw(16, 4);
    RawProperty stx_uid = RawProperty.raw(20, 4);
    RawProperty stx_gid = RawProperty.raw(24, 4);
    RawProperty stx_mode = RawProperty.raw(28, 2);
    RawProperty stx_ino = RawProperty.raw(32, 8);
    RawProperty stx_size = RawProperty.raw(40, 8);
    RawProperty stx_blocks = RawProperty.raw(48, 8);
    RawProperty stx_attributes_mask = RawProperty.raw(56, 8);
    RawProperty stx_rdev_major = RawProperty.raw(128, 4);
    RawProperty stx_rdev_minor = RawProperty.raw(132, 4);
    RawProperty stx_dev_major = RawProperty.raw(136, 4);
    RawProperty stx_dev_minor = RawProperty.raw(140, 4);
    RawProperty stx_atime = RawProperty.raw(64, 16);
    RawProperty stx_btime = RawProperty.raw(80, 16);
    RawProperty stx_ctime = RawProperty.raw(96, 16);
    RawProperty stx_mtime = RawProperty.raw(112, 16);
}