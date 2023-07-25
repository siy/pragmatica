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
 */

package org.pragmatica.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pragmatica.io.async.Timeout;
import org.pragmatica.io.async.common.OffsetT;
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.file.FilePermission;
import org.pragmatica.io.async.file.OpenFlags;
import org.pragmatica.io.async.net.*;
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.lang.*;

import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.pragmatica.io.async.Proactor.proactor;
import static org.pragmatica.io.async.util.Units._1KiB;
import static org.pragmatica.lang.Option.empty;
import static org.pragmatica.lang.Option.option;

//TODO: remaining tests
class ProactorTest {
    @Test
    void nopCanBeSubmitted() {
        var promise = Promise.<Unit>promise(p -> proactor().nop(p::resolve));

        promise.onSuccess(Assertions::assertNotNull)
               .onSuccess(System.out::println)
               .onFailure(ProactorTest::fail)
               .join();
    }

    @Test
    void delayCanBeSubmitted() {
        var finalResult = new AtomicReference<Result<Duration>>();
        proactor().delay(finalResult::set, Timeout.timeout(10).millis());

        waitForResult(finalResult);

        finalResult.get()
                   .onFailure(ProactorTest::fail)
                   .onSuccess(duration -> System.out.println("Duration: " + duration.toMillis() + " milliseconds"))
                   .onSuccess(duration -> assertTrue(duration.compareTo(Duration.ofMillis(10)) > 0));
    }

    @Test
    void fileCanBeOpenedReadAndClosed() {
        var fileDescriptor = new AtomicReference<Result<FileDescriptor>>();
        var fileName = "src/test/resources/english-wiki.test.data";

        proactor().open(fileDescriptor::set,
                        Path.of(fileName),
                        EnumSet.of(OpenFlags.READ_ONLY),
                        EnumSet.noneOf(FilePermission.class),
                        empty());

        waitForResult(fileDescriptor);

        fileDescriptor.get()
                      .onSuccess(fd -> System.out.println("Open successful: " + fd))
                      .onSuccess(fd -> assertTrue(fd.descriptor() > 0))
                      .onSuccess(fd -> {
                          var readResult = new AtomicReference<Result<SizeT>>();

                          try (var buffer1 = OffHeapSlice.fixedSize(_1KiB)) {
                              proactor().readVector(readResult::set, fd, buffer1);
                              waitForResult(readResult);

                              readResult.get()
                                        .onSuccess(sz -> System.out.println("Successfully read " + sz + " bytes"))
                                        .onFailure(ProactorTest::fail);
                          }
                      })
                      .onFailure(ProactorTest::fail);


        var closeResult = new AtomicReference<Result<Unit>>();
        fileDescriptor.get()
                      .onSuccess(fd -> proactor().close((closeResult::set), fd, empty()));

        waitForResult(closeResult);

        closeResult.get()
                   .onSuccess(unit -> System.out.println("Close successful " + unit))
                   .onFailure(ProactorTest::fail);
    }

    @Test
    void externalHostCanBeConnectedAndRead() throws UnknownHostException {
        var addr = java.net.Inet4Address.getByName("www.google.com");
        var address = InetAddress.inet4Address(addr.getAddress())
                                 .fold(ProactorTest::throwIfError,
                                       inetAddress -> SocketAddress.socketAddress(InetPort.inetPort(80), inetAddress));

        System.out.println("Address: " + address);

        try (var preparedText = OffHeapSlice.fromBytes("GET /\n".getBytes(StandardCharsets.US_ASCII));
             var buffer = proactor().allocateFixedBuffer(768).fold(ProactorTest::throwIfError, Functions::id)) {

            var socketResult = new AtomicReference<Result<FileDescriptor>>();
            proactor().socket(socketResult::set,
                              AddressFamily.INET,
                              SocketType.STREAM,
                              SocketFlag.none(),
                              SocketOption.reuseAll());

            waitForResult(socketResult);
            socketResult.get()
                        .onFailure(failure -> Assertions.fail(failure::message))
                        .onSuccess(fd -> {
                            var connectResult = new AtomicReference<Result<FileDescriptor>>();

                            proactor().connect(connectResult::set,
                                               fd, address, Option.option(Timeout.timeout(1).seconds()));

                            waitForResult(connectResult);

                            connectResult.get()
                                         .onFailure(ProactorTest::fail)
                                         .onSuccess(r1 -> System.out.println("Socket connected: " + r1));

                            var writeResult = new AtomicReference<Result<SizeT>>();
                            proactor().write(writeResult::set,
                                             fd, preparedText, OffsetT.ZERO, option(Timeout.timeout(1).seconds()));

                            waitForResult(writeResult);

                            writeResult.get()
                                       .onFailure(ProactorTest::fail)
                                       .onSuccess(sizeT -> System.out.println("Wrote " + sizeT + " bytes"));

                            var readResult = new AtomicReference<Result<SizeT>>();

                            proactor().readFixed(readResult::set,
                                                 fd, buffer, OffsetT.ZERO, option(Timeout.timeout(1).seconds()));

                            waitForResult(readResult);

                            var closeResult = new AtomicReference<Result<Unit>>();
                            proactor().close(closeResult::set, fd, empty());

                            waitForResult(closeResult);

                            System.out.println("Content: [" + new String(buffer.export(), StandardCharsets.UTF_8) + "]");
                        });
        }
    }

    private static <T> T throwIfError(Result.Cause f) {
        fail(f);
        return null;
    }

    private void waitForResult(AtomicReference<?> reference) {
        do {
        } while (reference.get() == null);
    }

    private static void fail(Result.Cause failure) {
        Assertions.fail(failure.message());
    }
}