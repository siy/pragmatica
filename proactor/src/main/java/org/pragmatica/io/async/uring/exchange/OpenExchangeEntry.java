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

package org.pragmatica.io.async.uring.exchange;

import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.SystemError;
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.uring.struct.offheap.OffHeapCString;
import org.pragmatica.io.async.uring.struct.raw.SQEntry;
import org.pragmatica.io.async.uring.utils.PlainObjectPool;
import org.pragmatica.lang.Result;

import java.nio.file.Path;
import java.util.function.BiConsumer;

import static org.pragmatica.io.async.uring.AsyncOperation.IORING_OP_OPENAT;
import static org.pragmatica.lang.Result.success;

/**
 * Exchange entry for {@code open} request.
 */
public class OpenExchangeEntry extends AbstractExchangeEntry<OpenExchangeEntry, FileDescriptor> {
    private static final int AT_FDCWD = -100; // Special value used to indicate the openat/statx functions should use the current working directory.

    private OffHeapCString rawPath;
    private byte flags;
    private int openFlags;
    private int mode;

    protected OpenExchangeEntry(PlainObjectPool<OpenExchangeEntry> pool) {
        super(IORING_OP_OPENAT, pool);
    }

    @Override
    protected void doAccept(int res, int flags, Proactor proactor) {
        rawPath.dispose();
        rawPath = null;

        var result = res < 0
                     ? SystemError.<FileDescriptor>result(res)
                     : success(FileDescriptor.file(res));
        completion.accept(result, proactor);
    }

    @Override
    public SQEntry apply(SQEntry entry) {
        return super.apply(entry)
                    .flags(flags)
                    .fd(AT_FDCWD)
                    .addr(rawPath.address())
                    .len(mode)
                    .openFlags(openFlags);
    }

    public OpenExchangeEntry prepare(BiConsumer<Result<FileDescriptor>, Proactor> completion, Path path, int openFlags, int mode, byte flags) {
        rawPath = OffHeapCString.cstring(path.toString());

        this.flags = flags;
        this.openFlags = openFlags;
        this.mode = mode;

        return super.prepare(completion);
    }
}
