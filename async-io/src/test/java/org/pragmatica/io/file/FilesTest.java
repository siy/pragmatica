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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.util.OffHeapBuffer;
import org.pragmatica.io.codec.UTF8Decoder;
import org.pragmatica.lang.Unit;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.pragmatica.io.file.Files.blocks;
import static org.pragmatica.lang.Promise.all;

class FilesTest {
    private static final int _1M = 1024 * 1024;

    @Test
    void forEachBlockPassesAllChunks() {
        var fileName = Path.of("target/classes", Files.class.getName().replace('.', '/') + ".class");
        var size1 = new AtomicLong(0);
        var size2 = new AtomicLong(0);

        all(blocks(SizeT.sizeT(512), fileName, buffer -> size1.addAndGet(buffer.used())),
            blocks(SizeT.sizeT(_1M), fileName, buffer1 -> size2.addAndGet(buffer1.used())))
            .map((_1, _2) -> {
                assertEquals(size1.get(), size2.get());
                return Unit.unit();
            })
            .join()
            .onFailureDo(Assertions::fail);
    }

    @Test
    void readUTF8File() {
        var fileName = Path.of("src/test/resources/utf8/chinese-wiki.html");
        var reference = new AtomicReference<String>(null);

        var promise = blocks(SizeT.sizeT(1024 * 1024), fileName, buffer1 -> reference.set(decode(buffer1)));

        promise.join().onFailureDo(Assertions::fail);
        assertNotNull(reference.get());
    }

    private String decode(OffHeapBuffer buffer1) {
        var builder = new StringBuilder(_1M);
        var decoder = new UTF8Decoder();

        decoder.decodeWithRecovery(buffer1, builder);

        return builder.toString();
    }
}