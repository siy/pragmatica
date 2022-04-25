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

package org.pragmatica.lang;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.pragmatica.io.async.common.OffsetT;
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.net.*;
import org.pragmatica.io.async.util.OffHeapSlice;

import java.net.UnknownHostException;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.pragmatica.io.async.Timeout.timeout;
import static org.pragmatica.io.async.file.FilePermission.none;
import static org.pragmatica.io.async.file.OpenFlags.readOnly;

public class PromiseIOTest {
    @Test
    void nopCanBeSubmitted() {
        var result = Promise.<Unit>promise((p1, proactor) -> proactor.nop(p1::resolve))
                            .join();

        assertEquals(Unit.unitResult(), result);
    }

    @Test
    void delayCanBeSubmitted() {
        var delay = 100;
        var result = Promise.<Duration>promise((p1, proactor) -> proactor.delay(p1::resolve, timeout(delay).millis()))
                            .join();

        result.onSuccess(duration -> assertTrue(duration.compareTo(Duration.ofMillis(delay)) >= 0))
              .onFailure(PromiseIOTest::fail);
    }

    @Test
    void fileCanBeOpenReadAndClosed() {
        var fileName = Path.of("target/classes", Promise.class.getName().replace('.', '/') + ".class");


        var openResult = Promise.<FileDescriptor>promise(
                                    (promise, proactor) -> proactor.open(promise::resolve, fileName, readOnly(), none(), Option.empty()))
                                .join();

        openResult.onFailure(PromiseIOTest::fail)
                  .onSuccess(fd -> System.out.println("Open successful: " + fd))
                  .onSuccess(fd -> {
                      try (var buffer = OffHeapSlice.fixedSize(128)) {
                          Promise.<SizeT>promise((promise, proactor) -> proactor.read(promise::resolve, fd, buffer, OffsetT.ZERO, Option.empty()))
                                 .join()
                                 .onFailure(PromiseIOTest::fail)
                                 .onSuccess(sizeT -> System.out.println("Read " + sizeT + " bytes"))
                                 .onSuccess(sizeT -> assertTrue(sizeT.compareTo(SizeT.ZERO) > 0));

                          System.out.println("Buffer content: " + buffer.hexDump());
                      }
                  })
                  .flatMap(fd -> Promise.<Unit>promise((promise, proactor) -> proactor.close(promise::resolve, fd, Option.empty())).join())
                  .onFailure(PromiseIOTest::fail);
    }

    @Test
    @Disabled("Incomplete")
    void udpCanBeSentAndReceived() throws UnknownHostException {
        var address = InetAddress.inet4Address(java.net.Inet4Address.getByName("www.google.com").getAddress())
                                 .fold(PromiseIOTest::fail,
                                       inetAddress -> SocketAddress.socketAddress(InetPort.inetPort(80), inetAddress));

        PromiseIO.socket(AddressFamily.INET, SocketType.DGRAM, SocketFlag.closeOnExec(), SocketOption.reuseAll())
                 .flatMap(socket -> PromiseIO.connect(socket, address))
                 .flatMap(socket -> {
                     try (var buffer = OffHeapSlice.fixedSize(512)) {


                         return PromiseIO.write(socket, buffer)
                                         .onFailure(PromiseIOTest::fail)
                                         .flatMap(size -> PromiseIO.read(socket, buffer))
                                         .onFailure(PromiseIOTest::fail)
                                         .flatMap(__ -> Promise.successful(socket))
                                         .onFailure(PromiseIOTest::fail);
                     }

                 })
                 .flatMap(PromiseIO::close);
    }

    private static <T> T fail(Cause cause) {
        Assertions.fail(cause.message());
        return null;
    }
}
