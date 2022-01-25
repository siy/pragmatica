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
import org.pragmatica.io.async.file.FilePermission;
import org.pragmatica.io.async.file.OpenFlags;
import org.pragmatica.io.async.util.OffHeapBuffer;
import org.pragmatica.lang.*;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;

public final class Files {
    /**
     * Read specified file in chunks of specified size and pass them to provided consumer. Note that last chunk might be shorter than requested size.
     *
     * @param blockSize Chunk size
     * @param path      Path to file
     * @param openFlags File open flags. Refer to {@link OpenFlags} for more details
     * @param timeout   Timeout for each internal operation - open, each read and close. Same value is used for each operation
     * @param consumer  Consumer which will receive file chunks
     *
     * @return Promise instance which will be resolved once last chunk will be passed to consumer or in case of error.
     */
    public static Promise<Unit> forEachBlock(SizeT blockSize,
                                             Path path,
                                             Set<OpenFlags> openFlags,
                                             Option<Timeout> timeout,
                                             Consumer<OffHeapBuffer> consumer) {

        return PromiseIO.open(path, openFlags, FilePermission.none(), timeout)
                        .flatMap(fd -> new FileReaderProtocol(fd, blockSize, consumer, timeout)
                            .run()
                            .onResult(__ -> PromiseIO.close(fd, timeout)));
    }

    /**
     * Same as {@link #forEachBlock(SizeT, Path, Set, Option, Consumer)} except file is opened in read-only mode.
     *
     * @param blockSize Chunk size
     * @param path      Path to file
     * @param timeout   Timeout for each internal operation - open, each read and close. Same value is used for each operation
     * @param consumer  Consumer which will receive file chunks
     *
     * @return Promise instance which will be resolved once last chunk will be passed to consumer or in case of error.
     */
    public static Promise<Unit> forEachBlock(SizeT blockSize, Path path, Option<Timeout> timeout, Consumer<OffHeapBuffer> consumer) {
        return forEachBlock(blockSize, path, OpenFlags.readOnly(), timeout, consumer);
    }

    /**
     * Same as {@link #forEachBlock(SizeT, Path, Set, Option, Consumer)} except file is opened in read-only mode
     * and no timeouts are applied to internal operations.
     *
     * @param blockSize Chunk size
     * @param path      Path to file
     * @param consumer  Consumer which will receive file chunks
     *
     * @return Promise instance which will be resolved once last chunk will be passed to consumer or in case of error.
     */
    public static Promise<Unit> forEachBlock(SizeT blockSize, Path path, Consumer<OffHeapBuffer> consumer) {
        return forEachBlock(blockSize, path, OpenFlags.readOnly(), Option.empty(), consumer);
    }

    private static final class FileReaderProtocol {
        private final FileDescriptor fd;
        private final Consumer<OffHeapBuffer> consumer;
        private final Option<Timeout> timeout;
        private final OffHeapBuffer buffer;
        private final Promise<Unit> promise;
        private long offset = 0;

        private FileReaderProtocol(FileDescriptor fd,
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
}
