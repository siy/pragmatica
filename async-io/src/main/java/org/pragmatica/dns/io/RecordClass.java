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

package org.pragmatica.dns.io;

import org.pragmatica.lang.Result;

import static org.pragmatica.lang.Result.success;
import static org.pragmatica.dns.io.DnsIoErrors.INVALID_RECORD_CLASS;

public enum RecordClass {
    IN(1),
    CS(2),
    CH(3),
    HS(4),
    NONE(254),
    ANY(255);

    private final short value;

    RecordClass(int value) {
        this.value = (short) value;
    }

    public short toShort() {
        return this.value;
    }

    public static Result<RecordClass> toRecordClass(short value) {
        return switch (value) {
            case 1 -> success(IN);
            case 2 -> success(CS);
            case 3 -> success(CH);
            case 4 -> success(HS);
            case 254 -> success(NONE);
            case 255 -> success(ANY);
            default -> INVALID_RECORD_CLASS.result();
        };
    }
}
