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
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.utils.Causes;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Unit;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.pragmatica.io.async.Proactor.proactor;

/**
 * File reading protocol which implements sequential reading of the file in chunks of fixed size. Last chunk might be shorter than requested size.
 */
public interface BlockReaderProtocol {
    static BlockReaderProtocol blockReaderProtocol(FileDescriptor fd,
                                                   SizeT bufferSize,
                                                   Consumer<OffHeapSlice> consumer,
                                                   Option<Timeout> timeout) {

        var buffer = OffHeapSlice.fixedSize((int) bufferSize.value());
        var promise = Promise.<Unit>promise().onResultDo(buffer::close);
        var context = new BlockReaderProtocolContext(fd, consumer, timeout, buffer, promise);

        return new BlockReaderProtocolImpl(context);
    }

    Promise<Unit> read();
}

record BlockReaderProtocolContext(FileDescriptor fd, Consumer<OffHeapSlice> consumer, Option<Timeout> timeout, OffHeapSlice buffer,
                                  Promise<Unit> promise) {}

final class BlockReaderProtocolImpl implements BlockReaderProtocol, BiConsumer<Result<SizeT>, Proactor>, Runnable {
    private final BlockReaderProtocolContext context;
    private long offset = 0;

    public BlockReaderProtocolImpl(BlockReaderProtocolContext context) {
        this.context = context;
    }

    public Promise<Unit> read() {
        Promise.runAsync(this);

        return context.promise();
    }

    @Override
    public void run() {
        proactor().read(this, context.fd(), context.buffer(), OffsetT.offsetT(offset), context.timeout());
    }

    @Override
    public void accept(Result<SizeT> result, Proactor unused) {
        result.onFailure(context.promise()::failure)
              .onSuccess(offsetT -> offset += offsetT.value())
              .onSuccessDo(() -> context.consumer().accept(context.buffer()))
              .filter(Causes.IRRELEVANT, size -> size.value() == context.buffer().size())
              .onSuccessDo(this)
              .onFailureDo(() -> context.promise().resolve(Unit.unitResult()));
    }
}
