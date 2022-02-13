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

package org.pragmatica.io.file;

import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.Timeout;
import org.pragmatica.io.async.common.OffsetT;
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.util.OffHeapBuffer;
import org.pragmatica.lang.Causes;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import java.util.function.Consumer;

public final class BlockReaderProtocol {
    private final FileDescriptor fd;
    private final Consumer<OffHeapBuffer> consumer;
    private final Option<Timeout> timeout;
    private final OffHeapBuffer buffer;
    private final Promise<Unit> promise;
    private long offset = 0;

    BlockReaderProtocol(FileDescriptor fd,
                        SizeT bufferSize,
                        Consumer<OffHeapBuffer> consumer,
                        Option<Timeout> timeout) {
        this.fd = fd;
        this.consumer = consumer;
        this.timeout = timeout;
        this.buffer = OffHeapBuffer.fixedSize((int) bufferSize.value());
        this.promise = Promise.promise();
    }

    public Promise<Unit> run() {
        promise.async((__, proactor) -> processChunk(proactor));

        return promise;
    }

    private void processChunk(Proactor proactor) {
        proactor.read((result, proactor1) -> {
            result.onFailure(promise::failure)
                  .onSuccess(offsetT -> offset += offsetT.value())
                  .onSuccess(__ -> consumer.accept(buffer))
                  .filter(Causes.IRRELEVANT, size -> size.value() == buffer.size())
                  .onSuccess(__ -> processChunk(proactor1))
                  .onFailure(__ -> promise.resolve(Unit.unitResult()));
        }, fd, buffer, OffsetT.offsetT(offset), timeout);
    }
}
