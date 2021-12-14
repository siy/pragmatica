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
import org.pfj.io.async.Proactor;
import org.pfj.io.async.common.OffsetT;
import org.pfj.io.async.common.SizeT;
import org.pfj.io.async.file.FileDescriptor;
import org.pfj.io.async.file.FilePermission;
import org.pfj.io.async.file.OpenFlags;
import org.pfj.io.async.net.*;
import org.pfj.io.async.util.OffHeapBuffer;
import org.pfj.io.async.Timeout;
import org.pfj.lang.Option;
import org.pfj.lang.Result;
import org.pfj.io.async.net.AddressFamily;
import org.pfj.io.async.net.InetPort;
import org.pfj.io.async.net.SocketAddressIn;
import org.pfj.io.async.net.SocketFlag;
import org.pfj.io.async.net.SocketOption;
import org.pfj.io.async.net.SocketType;
import org.pfj.lang.Unit;

import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.pfj.lang.Option.empty;
import static org.pfj.lang.Option.option;

//TODO: remaining tests
class ProactorTest {
    private final Proactor proactor = Proactor.proactor();

    @Test
    void nopCanBeSubmitted() {
        final var finalResult = new AtomicReference<Result<?>>();

        proactor.nop((result, __) -> finalResult.set(result));

        waitForResult(finalResult);

        finalResult.get()
            .onSuccess(Assertions::assertNotNull)
            .onSuccess(System.out::println)
            .onFailure(__ -> fail());
    }

    @Test
    void delayCanBeSubmitted() {
        final var finalResult = new AtomicReference<Result<Duration>>();
        proactor.delay((result, __) -> finalResult.set(result), Timeout.timeout(100).millis());

        waitForResult(finalResult);

        finalResult.get()
            .onFailure(failure -> fail(failure::message))
            .onSuccess(duration -> System.out.println("Duration: " + duration.toMillis() + " milliseconds"))
            .onSuccess(duration -> assertTrue(duration.compareTo(Duration.ofMillis(100)) > 0));
    }

    @Test
    void fileCanBeOpenedAndClosed() {
        final var fileDescriptor = new AtomicReference<Result<FileDescriptor>>();
        final var fileName = "target/classes/" + Proactor.class.getName().replace('.', '/') + ".class";

        System.out.println("Trying to open " + fileName);

        proactor.open((result, __) -> fileDescriptor.set(result),
            Path.of(fileName),
            EnumSet.of(OpenFlags.READ_ONLY),
            EnumSet.noneOf(FilePermission.class),
            empty());

        waitForResult(fileDescriptor);

        fileDescriptor.get()
            .onSuccess(fd -> System.out.println("Open successful: " + fd))
            .onSuccess(fd -> assertTrue(fd.descriptor() > 0))
            .onFailure(f -> fail(f::message));

        final var closeResult = new AtomicReference<Result<Unit>>();
        fileDescriptor.get()
            .onSuccess(fd -> proactor.closeFileDescriptor(((result, __) -> closeResult.set(result)), fd, empty()));

        waitForResult(closeResult);

        closeResult.get()
            .onSuccess(unit -> System.out.println("Close successful " + unit))
            .onFailure(f -> fail(f::message));
    }

    @Test
    void externalHostCanBeConnectedAndRead() throws UnknownHostException {
        final var addr = java.net.Inet4Address.getByName("www.google.com");
        final var address = Inet4Address.inet4Address(addr.getAddress())
            .fold(failure -> fail(failure::message),
                inetAddress -> SocketAddressIn.create(InetPort.inetPort(80),
                    inetAddress));

        System.out.println("Address: " + address);

        try (final OffHeapBuffer preparedText = OffHeapBuffer.fromBytes("GET /\n".getBytes(StandardCharsets.US_ASCII))) {
            try (final OffHeapBuffer buffer = OffHeapBuffer.fixedSize(768)) {
                buffer.clear().used(buffer.size());

                var socketResult = new AtomicReference<Result<FileDescriptor>>();
                proactor.socket((result, __) -> socketResult.set(result),
                    AddressFamily.INET,
                    SocketType.STREAM,
                    SocketFlag.none(),
                    SocketOption.reuseAll());

                waitForResult(socketResult);
                socketResult.get()
                    .onFailure(failure -> Assertions.fail(failure::message))
                    .onSuccess(fd -> {
                        var connectResult = new AtomicReference<Result<FileDescriptor>>();

                        proactor.connect((result, __) -> connectResult.set(result),
                            fd, address, Option.option(Timeout.timeout(1).seconds()));

                        waitForResult(connectResult);

                        connectResult.get()
                            .onFailure(failure -> fail(failure::message))
                            .onSuccess(r1 -> System.out.println("Socket connected: " + r1));

                        var writeResult = new AtomicReference<Result<SizeT>>();
                        proactor.write(((result, __) -> writeResult.set(result)),
                            fd, preparedText, OffsetT.ZERO, option(Timeout.timeout(1).seconds()));

                        waitForResult(writeResult);

                        writeResult.get()
                            .onFailure(failure -> fail(failure::message))
                            .onSuccess(sizeT -> System.out.println("Wrote " + sizeT + " bytes"));

                        var readResult = new AtomicReference<Result<SizeT>>();

                        proactor.read(((result, __) -> readResult.set(result)),
                            fd, buffer, OffsetT.ZERO, option(Timeout.timeout(1).seconds()));

                        waitForResult(readResult);

                        var closeResult = new AtomicReference<Result<Unit>>();
                        proactor.closeFileDescriptor(((result, __) -> closeResult.set(result)), fd, empty());

                        waitForResult(closeResult);

                        System.out.println("Content: [" + new String(buffer.export(), StandardCharsets.UTF_8) + "]");
                    });
            }
        }
    }

    private void waitForResult(AtomicReference<?> reference) {
        do {
            proactor.processIO(); //For submission
            proactor.processIO(); //For completion
        } while (reference.get() == null);
    }
}