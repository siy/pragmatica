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

package org.pfj.io.async.uring.exchange;

import org.pfj.io.async.uring.struct.raw.SubmitQueueEntry;
import org.pfj.io.async.SystemError;
import org.pfj.io.async.Proactor;
import org.pfj.io.async.file.FileDescriptor;
import org.pfj.io.async.uring.struct.offheap.OffHeapCString;
import org.pfj.io.async.uring.utils.PlainObjectPool;
import org.pfj.lang.Result;

import java.nio.file.Path;
import java.util.function.BiConsumer;

import static org.pfj.io.async.uring.AsyncOperation.IORING_OP_OPENAT;
import static org.pfj.lang.Result.success;

public class OpenExchangeEntry extends AbstractExchangeEntry<OpenExchangeEntry, FileDescriptor> {
    private static final int AT_FDCWD = -100; // Special value used to indicate the openat/statx functions should use the current working directory.

    private OffHeapCString rawPath;
    private byte flags;
    private int openFlags;
    private int mode;

    protected OpenExchangeEntry(final PlainObjectPool<OpenExchangeEntry> pool) {
        super(IORING_OP_OPENAT, pool);
    }

    @Override
    protected void doAccept(final int res, final int flags, final Proactor proactor) {
        rawPath.dispose();
        rawPath = null;

        final var result = res < 0
            ? SystemError.<FileDescriptor>result(res)
            : success(FileDescriptor.file(res));
        completion.accept(result, proactor);
    }

    @Override
    public SubmitQueueEntry apply(final SubmitQueueEntry entry) {
        return super.apply(entry)
            .flags(flags)
            .fd(AT_FDCWD)
            .addr(rawPath.address())
            .len(mode)
            .openFlags(openFlags);
    }

    public OpenExchangeEntry prepare(final BiConsumer<Result<FileDescriptor>, Proactor> completion,
                                     final Path path,
                                     final int openFlags,
                                     final int mode,
                                     final byte flags) {
        rawPath = OffHeapCString.cstring(path.toString());

        this.flags = flags;
        this.openFlags = openFlags;
        this.mode = mode;

        return super.prepare(completion);
    }
}
