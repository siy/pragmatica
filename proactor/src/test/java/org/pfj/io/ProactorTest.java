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

package org.pfj.io;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pfj.io.async.common.OffsetT;
import org.pfj.io.async.file.FilePermission;
import org.pfj.io.async.file.OpenFlags;
import org.pfj.io.async.net.*;
import org.pfj.io.async.util.OffHeapBuffer;
import org.pfj.io.scheduler.Timeout;
import org.pfj.lang.Result;
import org.pfj.io.async.net.AddressFamily;
import org.pfj.io.async.net.InetPort;
import org.pfj.io.async.net.SocketAddressIn;
import org.pfj.io.async.net.SocketFlag;
import org.pfj.io.async.net.SocketOption;
import org.pfj.io.async.net.SocketType;

import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

//TODO: remaining tests
class ProactorTest {
    private final Proactor proactor = Proactor.proactor();

    @Test
    void nopCanBeSubmitted() {
        final var finalResult = new AtomicReference<Result<?>>();
//        final var promise = proactor.nop(Promise.promise())
//                                    .onResult(Assertions::assertNotNull)
//                                    .onResult(System.out::println)
//                                    .onResult(finalResult::set);
//        waitForResult(promise);
//        finalResult.get().onFailure($ -> fail());
    }

//    @Test
//    void delayCanBeSubmitted() {
//        final var finalResult = new AtomicReference<Result<Duration>>();
//        proactor.delay((result, submitter) -> finalResult.set(result), Timeout.timeout(100).millis());
//        do {
//            proactor.processIO(); //For submission
//            proactor.processIO(); //For completion
//        } while (finalResult.get() == null);
//
//        finalResult.get()
//                   .onFailure($ -> fail())
//                   .onSuccess(duration -> assertTrue(duration.compareTo(Duration.ofMillis(100)) > 0));
//    }
//
//    @Test
//    void fileCanBeOpenedAndClosed() {
//        final var finalResult = new AtomicReference<Result<?>>();
//        final var promise = proactor.open(Path.of("target/classes/org/reactivetoolbox/io/Proactor.class"),
//                                          EnumSet.of(OpenFlags.READ_ONLY),
//                                          EnumSet.noneOf(FilePermission.class),
//                                          empty())
//                                    .onResult(System.out::println)
//                                    .onResult(v -> v.onSuccess(fd -> assertTrue(fd.descriptor() > 0))
//                                                    .onFailure(f -> fail()))
//                                    .flatMap(fd -> proactor.closeFileDescriptor(fd, empty()))
//                                    .onResult(System.out::println)
//                                    .onResult(finalResult::set);
//
//        waitForResult(promise);
//        finalResult.get().onFailure($ -> fail());
//    }
//
//    @Test
//    void fileCanBeOpenedReadAndClosed() {
//        final var finalResult = new AtomicReference<Result<?>>();
//        try (final OffHeapBuffer buffer = OffHeapBuffer.fixedSize(1024 * 1024)) {
//            final var promise = proactor.open(Path.of("target/classes/org/reactivetoolbox/io/Proactor.class"),
//                                              EnumSet.of(OpenFlags.READ_ONLY),
//                                              EnumSet.noneOf(FilePermission.class),
//                                              empty())
//                                        .onResult(System.out::println)
//                                        .onResult(v -> v.onSuccess(fd -> assertTrue(fd.descriptor() > 0))
//                                                        .onFailure(f -> fail()))
//                                        .flatMap(fd1 -> proactor.read(fd1, buffer, OffsetT.ZERO, empty()).map(sz -> tuple(fd1, sz)))
//                                        .onResult(System.out::println)
//                                        .flatMap(fdSz -> fdSz.map((fd, sz) -> proactor.closeFileDescriptor(fd, empty())))
//                                        .onResult(System.out::println)
//                                        .onResult(v -> v.onFailure(f -> fail()))
//                                        .onResult(finalResult::set);
//
//            waitForResult(promise);
//            finalResult.get().onFailure($ -> fail());
//        }
//    }
//
//    @Test
//    void externalHostCanBeConnectedAndRead() throws UnknownHostException {
//        final var finalResult = new AtomicReference<Result<?>>();
//        final var addr = java.net.Inet4Address.getByName("www.google.com");
//        final var address = Inet4Address.inet4Address(addr.getAddress()).fold($ -> fail(),
//                                                                 inetAddress -> SocketAddressIn.create(InetPort.inetPort(80),
//                                                                                                       inetAddress));
//
//        System.out.println("Address: " + address);
//
//        try (final OffHeapBuffer preparedText = OffHeapBuffer.fromBytes("GET /\n".getBytes(StandardCharsets.US_ASCII))) {
//            try (final OffHeapBuffer buffer = OffHeapBuffer.fixedSize(256)) {
//                buffer.clear().used(buffer.size());
//                final var promise = proactor.socket(AddressFamily.INET,
//                                                    SocketType.STREAM,
//                                                    SocketFlag.none(),
//                                                    SocketOption.reuseAll())
//                                            .onResult(r1 -> System.out.println("Socket created: " + r1))
//                                            .flatMap(fd -> proactor.connect(Promise.promise(), fd, address, option(Timeout.timeout(1).seconds()))
//                                                                   .onFailure($ -> proactor.closeFileDescriptor(fd, empty()))
//                                                                   .map(sz -> fd))
//                                            .onResult(r1 -> System.out.println("Socket connected: " + r1))
//                                            .flatMap(fd -> proactor.write(fd, preparedText, OffsetT.ZERO, option(Timeout.timeout(1).seconds()))
//                                                                   .map(sz -> fd))
//                                            .flatMap(fd -> proactor.read(fd, buffer, OffsetT.ZERO, option(Timeout.timeout(1).seconds()))
//                                                                   .onFailure($ -> proactor.closeFileDescriptor(fd, empty()))
//                                                                   .map(sz -> fd))
//                                            .onResult(System.out::println)
//                                            .flatMap(fd -> proactor.closeFileDescriptor(fd, empty()))
//                                            .onResult(finalResult::set);
//                waitForResult(promise);
//                finalResult.get().onFailure($ -> fail());
//
//                System.out.println("Content: [" + new String(buffer.export(), StandardCharsets.UTF_8) + "]");
//            }
//        }
//    }

//    private void waitForResult(final Promise<?> promise) {
//        final AtomicBoolean ready = new AtomicBoolean(false);
//
//        promise.onResult(v -> ready.lazySet(true));
//
//        do {
//            proactor.processIO(); //For submission
//            proactor.processIO(); //For completion
//        } while (!ready.get());
//
//        promise.syncWait();
//    }
}