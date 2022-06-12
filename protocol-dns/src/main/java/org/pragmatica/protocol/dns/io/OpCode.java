/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.pragmatica.protocol.dns.io;

import org.pragmatica.lang.Result;

import static org.pragmatica.lang.Result.success;
import static org.pragmatica.protocol.dns.io.DnsIoErrors.INVALID_OPCODE;

public enum OpCode {
    QUERY(0),
    IQUERY(1),
    STATUS(2),
    NOTIFY(4),
    UPDATE(5);

    private final byte value;

    OpCode(int value) {
        this.value = (byte) ((byte) (value & 0x0F) << 3);
    }

    public byte asByte() {
        return this.value;
    }

    public static Result<OpCode> fromByte(byte value) {
        return switch (value) {
            case 0 -> success(QUERY);
            case 1 -> success(IQUERY);
            case 2 -> success(STATUS);
            case 4 -> success(NOTIFY);
            case 5 -> success(UPDATE);
            default -> INVALID_OPCODE.result();
        };
    }
}
