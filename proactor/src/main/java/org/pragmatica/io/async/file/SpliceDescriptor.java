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

package org.pragmatica.io.async.file;

import org.pragmatica.io.async.common.OffsetT;
import org.pragmatica.io.async.common.SizeT;

import java.util.EnumSet;

/**
 * Container for all necessary details of the SPLICE operation.
 * <p>
 * This operation performs copying from one file descriptor to another completely at kernel space without involving any user space code or memory.
 */
public class SpliceDescriptor {
    private final FileDescriptor fromDescriptor;
    private final FileDescriptor toDescriptor;
    private final OffsetT fromOffset;
    private final OffsetT toOffset;
    private final SizeT bytesToCopy;
    private final EnumSet<SpliceFlags> flags;

    private SpliceDescriptor(final FileDescriptor fromDescriptor,
                             final FileDescriptor toDescriptor,
                             final OffsetT fromOffset,
                             final OffsetT toOffset,
                             final SizeT bytesToCopy,
                             final EnumSet<SpliceFlags> flags) {
        this.fromDescriptor = fromDescriptor;
        this.toDescriptor = toDescriptor;
        this.fromOffset = fromOffset;
        this.toOffset = toOffset;
        this.bytesToCopy = bytesToCopy;
        this.flags = flags;
    }

    /**
     * Source file descriptor.
     */
    public FileDescriptor fromDescriptor() {
        return fromDescriptor;
    }

    /**
     * Destination file descriptor.
     */
    public FileDescriptor toDescriptor() {
        return toDescriptor;
    }

    /**
     * Offset in the source file. Must be 0 if source file descriptor is a socket.
     */
    public OffsetT fromOffset() {
        return fromOffset;
    }

    /**
     * Offset in the destination file. Must be 0 if destination file descriptor is a socket.
     */
    public OffsetT toOffset() {
        return toOffset;
    }

    /**
     * Number of bytes to copy.
     */
    public SizeT bytesToCopy() {
        return bytesToCopy;
    }

    /**
     * Operation flags.
     */
    public EnumSet<SpliceFlags> flags() {
        return flags;
    }

    @Override
    public String toString() {
        return "SpliceDescriptor(" +
               "from: " + fromDescriptor +
               ", to: " + toDescriptor +
               ", fromOffset: " + fromOffset +
               ", toOffset: " + toOffset +
               ", toCopy: " + bytesToCopy +
               ", flags: " + flags +
               ')';
    }

    /**
     * Create new builder for assembling complete {@link SpliceDescriptor} instance.
     */
    public static SpliceDescriptorBuilder builder() {
        return fromDescriptor ->
            toDescriptor ->
                fromOffset ->
                    toOffset ->
                        bytesToCopy ->
                            flags -> new SpliceDescriptor(fromDescriptor, toDescriptor, fromOffset, toOffset, bytesToCopy, flags);
    }

    public interface SpliceDescriptorBuilder {
        Stage1 fromDescriptor(final FileDescriptor fromDescriptor);

        interface Stage1 {
            Stage2 toDescriptor(final FileDescriptor toDescriptor);
        }

        interface Stage2 {
            Stage3 fromOffset(final OffsetT fromOffset);
        }

        interface Stage3 {
            Stage4 toOffset(final OffsetT toOffset);
        }

        interface Stage4 {
            Stage5 bytesToCopy(final SizeT bytesToCopy);
        }

        interface Stage5 {
            SpliceDescriptor flags(final EnumSet<SpliceFlags> flags);
        }
    }
}
