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

package org.pragmatica.io.async.uring.struct.offheap;

import org.pragmatica.io.async.file.stat.FileStat;
import org.pragmatica.io.async.uring.struct.raw.RawStatx;
import org.pragmatica.io.async.uring.struct.shape.StatxOffsets;

/**
 * Representation of file status information.
 */
public class OffHeapFileStat extends AbstractOffHeapStructure<OffHeapFileStat> {
    private final RawStatx shape;

    private OffHeapFileStat() {
        super(StatxOffsets.SIZE);
        clear();
        shape = RawStatx.at(address());
    }

    public static OffHeapFileStat fileStat() {
        return new OffHeapFileStat();
    }

    public FileStat extract() {
        return shape.detach();
    }
}
