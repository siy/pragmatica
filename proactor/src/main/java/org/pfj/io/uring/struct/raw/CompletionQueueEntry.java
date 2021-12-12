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

package org.pfj.io.uring.struct.raw;

import org.pfj.io.uring.struct.AbstractExternalRawStructure;
import org.pfj.io.uring.struct.shape.CompletionQueueEntryOffsets;

import java.util.StringJoiner;

import static org.pfj.io.uring.struct.shape.CompletionQueueEntryOffsets.flags;
import static org.pfj.io.uring.struct.shape.CompletionQueueEntryOffsets.res;
import static org.pfj.io.uring.struct.shape.CompletionQueueEntryOffsets.user_data;

public class CompletionQueueEntry extends AbstractExternalRawStructure<CompletionQueueEntry> {
    private CompletionQueueEntry(final long address) {
        super(address, CompletionQueueEntryOffsets.SIZE);
    }

    public static CompletionQueueEntry at(final long address) {
        return new CompletionQueueEntry(address);
    }

    public long userData() {
        return getLong(user_data);
    }

    public int res() {
        return getInt(res);
    }

    public int flags() {
        return getInt(flags);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", "CompletionQueueEntry(", ")")
                .add("res: " + res())
                .add("flags: " + flags())
                .add("data: " + userData())
                .toString();
    }
}
