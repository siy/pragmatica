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

package org.pragmatica.io.async.file;

import org.pragmatica.io.async.common.OffsetT;
import org.pragmatica.io.async.common.SizeT;

import java.util.EnumSet;

/**
 * Container for all necessary details of the SPLICE operation.
 * <p>
 * This operation performs copying from one file descriptor to another completely at kernel space without involving any user space code or memory.
 * <p>
 * The descriptor contains following information:
 * <table>
 *     <tr> <th>Name</th> <th>Description</th> </tr>
 *     <tr>
 *         <td>fromDescriptor</td>
 *         <td>Source file descriptor or socket</td>
 *     </tr>
 *     <tr>
 *         <td>toDescriptor</td>
 *         <td>Destination file descriptor or socket</td>
 *     </tr>
 *     <tr>
 *         <td>fromOffset</td>
 *         <td>Offset in the source file if corresponding file descriptor belongs to file. Otherwise should be set to {@link OffsetT#ZERO}.</td>
 *     </tr>
 *     <tr>
 *         <td>toOffset</td>
 *         <td>Offset in the destination file if corresponding file descriptor belongs to file. Otherwise should be set to {@link OffsetT#ZERO}.</td>
 *     </tr>
 *     <tr>
 *         <td>bytesToCopy</td>
 *         <td>Number of bytes to copy. Note that only values below than 2<sup>32</sup> (unsigned int) are supported.</td>
 *     </tr>
 *     <tr>
 *         <td>flags</td>
 *         <td>Splice operation flags (see {@link SpliceFlags} for details)</td>
 *     </tr>
 * </table>
 */
public record SpliceDescriptor(FileDescriptor fromDescriptor, FileDescriptor toDescriptor, OffsetT fromOffset, OffsetT toOffset,
                               SizeT bytesToCopy, EnumSet<SpliceFlags> flags) {
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
        return
            fromDescriptor ->
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
