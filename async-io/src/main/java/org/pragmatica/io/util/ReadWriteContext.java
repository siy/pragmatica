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

package org.pragmatica.io.util;

import org.pragmatica.io.AsyncCloseable;
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.net.InetAddress;
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.lang.Functions.FN1;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.PromiseIO;
import org.pragmatica.lang.Unit;

import java.util.function.Consumer;

public final class ReadWriteContext<T extends InetAddress> implements AsyncCloseable {
    private final ClientConnectionContext<T> connectionContext;
    private final OffHeapSlice readBuffer;
    private final OffHeapSlice writeBuffer;

    private ReadWriteContext(ClientConnectionContext<T> connectionContext, int bufferSize) {
        this.connectionContext = connectionContext;
        this.readBuffer = OffHeapSlice.fixedSize(bufferSize);
        this.writeBuffer = OffHeapSlice.fixedSize(bufferSize);
    }

    public static <T extends InetAddress> ReadWriteContext<T> readWriteContext(ClientConnectionContext<T> connectionContext, int bufferSize) {
        return new ReadWriteContext<>(connectionContext, bufferSize);
    }

    public <R> Promise<R> read(FN1<R, OffHeapSlice> transformer) {
        return PromiseIO.read(connectionContext.socket(), readBuffer)
                        .mapReplace(() -> transformer.apply(readBuffer));
    }

    public Promise<SizeT> write(Consumer<OffHeapSlice> bufferFiller) {
        bufferFiller.accept(writeBuffer);
        return PromiseIO.write(connectionContext.socket(), writeBuffer);
    }

    @Override
    public Promise<Unit> close() {
        return connectionContext.close().onResultDo(() -> {
            readBuffer.close();
            writeBuffer.close();
        });
    }
}
