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

package org.pragmatica.io.file.protocol;

import org.pragmatica.io.async.util.OffHeapBuffer;
import org.pragmatica.io.codec.UTF8Decoder;

import java.util.function.Consumer;

public record LineReaderProtocol(long bufferSize, Consumer<String> consumer, StringBuilder stringBuilder, UTF8Decoder utf8Decoder)
    implements Consumer<OffHeapBuffer> {

    @Override
    public void accept(OffHeapBuffer offHeapBuffer) {
        utf8Decoder.decodeWithRecovery(offHeapBuffer, this::characterInput);

        if (offHeapBuffer.used() != bufferSize) {
            stripCarriageReturn();

            // Empty line or single trailing \r will be ignored
            if (stringBuilder.length() > 0) {
                consumer.accept(stringBuilder.toString());
            }
        }
    }

    private void characterInput(Character character) {
        if (character == '\n') {
            stripCarriageReturn();
            consumer.accept(stringBuilder.toString());
            stringBuilder.setLength(0);
        } else {
            stringBuilder.append(character);
        }
    }

    private void stripCarriageReturn() {
        if (stringBuilder.length() > 0 && stringBuilder.charAt(stringBuilder.length() - 1) == '\r') {
            stringBuilder.setLength(stringBuilder.length() - 1);
        }
    }
}
