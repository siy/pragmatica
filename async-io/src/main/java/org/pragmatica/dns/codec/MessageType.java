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

package org.pragmatica.dns.codec;

import org.pragmatica.lang.Result;

import static org.pragmatica.dns.codec.DnsIoErrors.INVALID_MESSAGE_TYPE;
import static org.pragmatica.lang.Result.success;

public enum MessageType {
    QUERY(0),
    RESPONSE(1);

    private final byte value;

    MessageType(int value) {
        this.value = (byte) ((byte) (value & 0x01) << 7);
    }

    public byte asByte() {
        return this.value;
    }

    public static Result<MessageType> fromByte(byte value) {
        return switch (value) {
            case 0 -> success(QUERY);
            case 1 -> success(RESPONSE);
            default -> INVALID_MESSAGE_TYPE.result();
        };
    }
}
