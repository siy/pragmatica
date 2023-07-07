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

package org.pragmatica.io.async.uring.struct.raw;

//TODO: move to enum
public class SubmitQueueEntryFlags {
    public static final byte IOSQE_FIXED_FILE = 0x001; /* use fixed fileset */
    public static final byte IOSQE_IO_DRAIN = 0x002; /* issue after inflight IO */
    public static final byte IOSQE_IO_LINK = 0x004; /* links next sqe */
    public static final byte IOSQE_IO_HARDLINK = 0x008; /* like LINK, but stronger */
    public static final byte IOSQE_ASYNC = 0x010; /* always go async */
    public static final byte IOSQE_BUFFER_SELECT = 0x020; /* select buffer from sqe->buf_group */
}
