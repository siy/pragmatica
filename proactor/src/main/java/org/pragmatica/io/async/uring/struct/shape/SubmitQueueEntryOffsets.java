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

package org.pragmatica.io.async.uring.struct.shape;

import org.pragmatica.io.async.util.raw.RawProperty;

public interface SubmitQueueEntryOffsets {
    int SIZE = 64;
    RawProperty opcode = RawProperty.raw(0, 1);
    RawProperty flags = RawProperty.raw(1, 1);
    RawProperty ioprio = RawProperty.raw(2, 2);
    RawProperty fd = RawProperty.raw(4, 4);
    RawProperty off = RawProperty.raw(8, 8);
    RawProperty addr2 = RawProperty.raw(8, 8);
    RawProperty addr = RawProperty.raw(16, 8);
    RawProperty splice_off_in = RawProperty.raw(16, 8);
    RawProperty len = RawProperty.raw(24, 4);
    RawProperty rw_flags = RawProperty.raw(28, 4);
    RawProperty fsync_flags = RawProperty.raw(28, 4);
    RawProperty poll_events = RawProperty.raw(28, 2);
    RawProperty sync_range_flags = RawProperty.raw(28, 4);
    RawProperty msg_flags = RawProperty.raw(28, 4);
    RawProperty timeout_flags = RawProperty.raw(28, 4);
    RawProperty accept_flags = RawProperty.raw(28, 4);
    RawProperty cancel_flags = RawProperty.raw(28, 4);
    RawProperty open_flags = RawProperty.raw(28, 4);
    RawProperty statx_flags = RawProperty.raw(28, 4);
    RawProperty fadvise_advice = RawProperty.raw(28, 4);
    RawProperty splice_flags = RawProperty.raw(28, 4);
    RawProperty user_data = RawProperty.raw(32, 8);
    RawProperty buf_index = RawProperty.raw(40, 2);
    RawProperty buf_group = RawProperty.raw(40, 2);
    RawProperty personality = RawProperty.raw(42, 2);
    RawProperty splice_fd_in = RawProperty.raw(44, 4);
}