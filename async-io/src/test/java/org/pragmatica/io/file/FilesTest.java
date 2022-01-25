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
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Unit;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FilesTest {
    @Test
    void forEachBlockPassesAllChunks() {
        var fileName = Path.of("target/classes", Promise.class.getName().replace('.', '/') + ".class");
        var size1 = new AtomicLong(0);
        var size2 = new AtomicLong(0);
        var first = Files.forEachBlock(SizeT.sizeT(512), fileName, buffer -> size1.addAndGet(buffer.used()));
        var second = Files.forEachBlock(SizeT.sizeT(1024 * 1024), fileName, buffer -> size2.addAndGet(buffer.used()));

        Promise.all(first, second)
               .map((_1, _2) -> {
                   assertEquals(size1.get(), size2.get());
                   return Unit.unit();
               })
               .join()
               .onFailureDo(Assertions::fail);
    }
}