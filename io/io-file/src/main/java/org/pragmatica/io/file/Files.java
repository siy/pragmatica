/*
 *  Copyright (c) 2020-2022 Sergiy Yevtushenko.
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

import org.pragmatica.io.PromiseIO;
import org.pragmatica.io.async.SystemError;
import org.pragmatica.io.async.Timeout;
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.file.FilePermission;
import org.pragmatica.io.async.file.OpenFlags;
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.io.codec.UTF8Decoder;
import org.pragmatica.io.file.protocol.BlockReaderProtocol;
import org.pragmatica.io.file.protocol.LineReaderProtocol;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import java.nio.file.Path;
import java.util.Set;
import java.util.function.Consumer;

import static org.pragmatica.io.file.protocol.BlockReaderProtocol.blockReaderProtocol;

/**
 * Useful file handling utilities.
 */
public final class Files {
    private static final SizeT DEFAULT_BUFFER_SIZE = SizeT.sizeT(16_384L);

    /**
     * Read specified file in chunks of specified size and pass them to provided consumer. The last chunk might be shorter than requested size.
     *
     * @param path      Path to file
     * @param blockSize Chunk size
     * @param openFlags File open flags. Refer to {@link OpenFlags} for more details
     * @param timeout   Timeout for each internal operation - open, each read and close. Same bufferSize is used for each operation
     * @param consumer  Consumer which will receive file chunks
     *
     * @return Promise instance which will be resolved once last chunk will be passed to consumer or in case of error.
     */
    public static Promise<Unit> blocks(Path path,
                                       SizeT blockSize,
                                       Set<OpenFlags> openFlags,
                                       Option<Timeout> timeout,
                                       Consumer<OffHeapSlice> consumer) {

        return PromiseIO.open(path, openFlags, FilePermission.none(), timeout)
                        .flatMap(fd -> blockReaderProtocol(fd, blockSize, consumer, timeout)
                            .read()
                            .onResult(__ -> PromiseIO.close(fd, timeout)));
    }

    /**
     * Same as {@link #blocks(Path, SizeT, Set, Option, Consumer)}, except buffer size is set to  {@link #DEFAULT_BUFFER_SIZE).
     *
     * @param path      Path to file
     * @param openFlags File open flags. Refer to {@link OpenFlags} for more details
     * @param timeout   Timeout for each internal operation - open, each read and close. Same bufferSize is used for each operation
     * @param consumer  Consumer which will receive file chunks
     *
     * @return Promise instance which will be resolved once last chunk will be passed to consumer or in case of error.
     */
    public static Promise<Unit> blocks(Path path,
                                       Set<OpenFlags> openFlags,
                                       Option<Timeout> timeout,
                                       Consumer<OffHeapSlice> consumer) {

        return blocks(path, DEFAULT_BUFFER_SIZE, openFlags, timeout, consumer);
    }

    /**
     * Same as {@link #blocks(Path, SizeT, Set, Option, Consumer)}, except file is opened in read-only mode.
     *
     * @param path      Path to file
     * @param blockSize Chunk size
     * @param timeout   Timeout for each internal operation - open, each read and close. Same bufferSize is used for each operation
     * @param consumer  Consumer which will receive file chunks
     *
     * @return Promise instance which will be resolved once last chunk will be passed to consumer or in case of error.
     */
    public static Promise<Unit> blocks(Path path, SizeT blockSize, Option<Timeout> timeout, Consumer<OffHeapSlice> consumer) {
        return blocks(path, blockSize, OpenFlags.readOnly(), timeout, consumer);
    }

    /**
     * Same as {@link #blocks(Path, SizeT, Option, Consumer)}, except buffer size is set to  {@link #DEFAULT_BUFFER_SIZE).
     *
     * @param path     Path to file
     * @param timeout  Timeout for each internal operation - open, each read and close. Same bufferSize is used for each operation
     * @param consumer Consumer which will receive file chunks
     *
     * @return Promise instance which will be resolved once last chunk will be passed to consumer or in case of error.
     */

    public static Promise<Unit> blocks(Path path, Option<Timeout> timeout, Consumer<OffHeapSlice> consumer) {
        return blocks(path, DEFAULT_BUFFER_SIZE, OpenFlags.readOnly(), timeout, consumer);
    }

    /**
     * Same as {@link #blocks(Path, SizeT, Set, Option, Consumer)}, except file is opened in read-only mode and no timeouts are applied to internal
     * operations.
     *
     * @param path      Path to file
     * @param blockSize Chunk size
     * @param consumer  Consumer which will receive file chunks
     *
     * @return Promise instance which will be resolved once last chunk will be passed to consumer or in case of error.
     */
    public static Promise<Unit> blocks(Path path, SizeT blockSize, Consumer<OffHeapSlice> consumer) {
        return blocks(path, blockSize, OpenFlags.readOnly(), Option.empty(), consumer);
    }

    /**
     * Same as {@link #blocks(Path, SizeT, Consumer)}, except buffer size is set to  {@link #DEFAULT_BUFFER_SIZE).
     *
     * @param path     Path to file
     * @param consumer Consumer which will receive file chunks
     *
     * @return Promise instance which will be resolved once last chunk will be passed to consumer or in case of error.
     */
    public static Promise<Unit> blocks(Path path, Consumer<OffHeapSlice> consumer) {
        return blocks(path, DEFAULT_BUFFER_SIZE, OpenFlags.readOnly(), Option.empty(), consumer);
    }

    /**
     * Read specified file, interpret it as a sequence of UTF-8 encoded characters separated into lines and submit them one by one into provided
     * consumer.
     *
     * @param path       Path to file.
     * @param bufferSize Size of buffer which should be used to read file.
     * @param openFlags  File open flags.
     * @param timeout    Timeout for all internal operations.
     * @param consumer   The consumer to submit lines to.
     *
     * @return Promise instance, which will be resolved once file end will be reached or in case of error.
     */
    public static Promise<Unit> lines(Path path,
                                      SizeT bufferSize,
                                      Set<OpenFlags> openFlags,
                                      Option<Timeout> timeout,
                                      Consumer<String> consumer) {
        var lineReaderProtocol = new LineReaderProtocol(bufferSize.value(), consumer, new StringBuilder(), new UTF8Decoder());

        return blocks(path, bufferSize, openFlags, timeout, lineReaderProtocol);
    }

    /**
     * Same as {@link #lines(Path, SizeT, Set, Option, Consumer)}, except buffer size is set to {@link #DEFAULT_BUFFER_SIZE).
     *
     * @param path      Path to file.
     * @param openFlags File open flags.
     * @param timeout   Timeout for all internal operations.
     * @param consumer  The consumer to submit lines to.
     *
     * @return Promise instance, which will be resolved once file end will be reached or in case of error.
     */
    public static Promise<Unit> lines(Path path, Set<OpenFlags> openFlags, Option<Timeout> timeout, Consumer<String> consumer) {
        return lines(path, DEFAULT_BUFFER_SIZE, openFlags, timeout, consumer);
    }

    /**
     * Same as {@link #lines(Path, SizeT, Set, Option, Consumer)}, except file is opened in read-only mode.
     *
     * @param path     Path to file.
     * @param timeout  Timeout for all internal operations.
     * @param consumer The consumer to submit lines to.
     *
     * @return Promise instance, which will be resolved once file end will be reached or in case of error.
     */
    public static Promise<Unit> lines(Path path, SizeT bufferSize, Option<Timeout> timeout, Consumer<String> consumer) {
        return lines(path, bufferSize, OpenFlags.readOnly(), timeout, consumer);
    }

    /**
     * Same as {@link #lines(Path, SizeT, Option, Consumer)}, except buffer size is set to {@link #DEFAULT_BUFFER_SIZE).
     *
     * @param path      Path to file.
     * @param openFlags File open flags.
     * @param timeout   Timeout for all internal operations.
     * @param consumer  The consumer to submit lines to.
     *
     * @return Promise instance, which will be resolved once file end will be reached or in case of error.
     */
    public static Promise<Unit> lines(Path path, Option<Timeout> timeout, Consumer<String> consumer) {
        return lines(path, DEFAULT_BUFFER_SIZE, OpenFlags.readOnly(), timeout, consumer);
    }

    /**
     * Same as {@link #lines(Path, SizeT, Set, Option, Consumer)}, except file is opened in read-only mode and no timeouts are applied to internal
     * operations.
     *
     * @param path       Path to file.
     * @param bufferSize Size of buffer which should be used to read file.
     * @param consumer   The consumer to submit lines to.
     *
     * @return Promise instance, which will be resolved once file end will be reached or in case of error.
     */
    public static Promise<Unit> lines(Path path, SizeT bufferSize, Consumer<String> consumer) {
        return lines(path, bufferSize, OpenFlags.readOnly(), Option.empty(), consumer);
    }

    /**
     * Same as {@link #lines(Path, SizeT, Consumer)}, except buffer size is set to {@link #DEFAULT_BUFFER_SIZE).
     *
     * @param path     Path to file.
     * @param consumer The consumer to submit lines to.
     *
     * @return Promise instance, which will be resolved once file end will be reached or in case of error.
     */
    public static Promise<Unit> lines(Path path, Consumer<String> consumer) {
        return lines(path, DEFAULT_BUFFER_SIZE, OpenFlags.readOnly(), Option.empty(), consumer);
    }


    //TODO: implement copy
    public enum FileCopyMode {
        APPEND,     // By default - replace content
        OVERWRITE,  // By default - fail if file exists
        COPY_PERMISSION, // By default - set ones provided in parameters
    }

    public static Promise<SizeT> copy(Path from, Path to, Set<FileCopyMode> mode, Set<FilePermission> destinationPermission, Option<Timeout> timeout) {
        // Not implemented yet
        return Promise.resolved(SystemError.EPERM.result());
    }


}
