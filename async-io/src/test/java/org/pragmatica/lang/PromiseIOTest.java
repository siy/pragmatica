/*
 *  Copyright (c) 2021 Sergiy Yevtushenko.
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
import org.junit.jupiter.api.Test;
import org.pragmatica.io.async.common.OffsetT;
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.util.OffHeapBuffer;

import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.pragmatica.io.async.Timeout.timeout;
import static org.pragmatica.io.async.file.FilePermission.none;
import static org.pragmatica.io.async.file.OpenFlags.readOnly;
import static org.pragmatica.lang.Option.empty;

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

    private static void fail(Cause cause) {
        Assertions.fail(cause.message());
    }

    @Test
    void fileCanBeOpenReadAndClosed() {
        var fileName = Path.of("target/classes", Promise.class.getName().replace('.', '/') + ".class");


        var openResult = Promise.<FileDescriptor>promise(
                                    (promise, proactor) -> proactor.open(promise::resolve, fileName, readOnly(), none(), empty()))
                                .join();

        openResult.onFailure(PromiseIOTest::fail)
                  .onSuccess(fd -> System.out.println("Open successful: " + fd))
                  .onSuccess(fd -> {
                      try (var buffer = OffHeapBuffer.fixedSize(128)) {
                          Promise.<SizeT>promise((promise, proactor) -> proactor.read(promise::resolve, fd, buffer, OffsetT.ZERO, empty()))
                                 .join()
                                 .onFailure(PromiseIOTest::fail)
                                 .onSuccess(sizeT -> System.out.println("Read " + sizeT + " bytes"))
                                 .onSuccess(sizeT -> assertTrue(sizeT.compareTo(SizeT.ZERO) > 0));

                          System.out.println("Buffer content: " + buffer.hexDump());
                      }
                  })
                  .flatMap(fd -> Promise.<Unit>promise((promise, proactor) -> proactor.close(promise::resolve, fd, empty())).join())
                  .onFailure(PromiseIOTest::fail);
    }
}
