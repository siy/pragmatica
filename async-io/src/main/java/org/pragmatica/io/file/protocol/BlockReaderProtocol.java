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

package org.pragmatica.io.file.protocol;

import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.Timeout;
import org.pragmatica.io.async.common.OffsetT;
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.lang.Causes;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import java.util.function.Consumer;

/**
 * File reading protocol which implements sequential reading of the file in chunks of fixed size.
 * Last chunk might be shorter than requested buffer size.
 */
public final class BlockReaderProtocol {
    private final FileDescriptor fd;
    private final Consumer<OffHeapSlice> consumer;
    private final Option<Timeout> timeout;
    private final OffHeapSlice buffer;
    private final Promise<Unit> promise;
    private long offset = 0;

    public BlockReaderProtocol(FileDescriptor fd,
                               SizeT bufferSize,
                               Consumer<OffHeapSlice> consumer,
                               Option<Timeout> timeout) {
        this.fd = fd;
        this.consumer = consumer;
        this.timeout = timeout;
        this.buffer = OffHeapSlice.fixedSize((int) bufferSize.value());
        this.promise = Promise.promise();
    }

    public Promise<Unit> run() {
        return promise.asyncIO(this::processChunk);
    }

    private void processChunk(Proactor proactor) {
        proactor.read((result, proactor1) -> {
            result.onFailure(promise::failure)
                  .onSuccess(offsetT -> offset += offsetT.value())
                  .onSuccessDo(() -> consumer.accept(buffer))
                  .filter(Causes.IRRELEVANT, size -> size.value() == buffer.size())
                  .onSuccessDo(() -> processChunk(proactor1))
                  .onFailureDo(() -> promise.resolve(Unit.unitResult()));
        }, fd, buffer, OffsetT.offsetT(offset), timeout);
    }
}
