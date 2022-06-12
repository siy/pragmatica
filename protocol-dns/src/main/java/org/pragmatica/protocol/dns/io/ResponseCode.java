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
import static org.pragmatica.protocol.dns.io.DnsIoErrors.INVALID_RESPONSE_CODE;

public enum ResponseCode {
    NO_ERROR(0),
    FORMAT_ERROR(1),
    SERVER_FAILURE(2),
    NAME_ERROR(3),
    NOT_IMPLEMENTED(4),
    REFUSED(5);

    private final byte value;

    ResponseCode(int value) {
        this.value = (byte) value;
    }

    public byte toByte() {
        return this.value;
    }
    public static Result<ResponseCode> fromByte(byte value) {
        return switch (value) {
            case 0 -> success(NO_ERROR);
            case 1 -> success(FORMAT_ERROR);
            case 2 -> success(SERVER_FAILURE);
            case 3 -> success(NAME_ERROR);
            case 4 -> success(NOT_IMPLEMENTED);
            case 5 -> success(REFUSED);
            default -> INVALID_RESPONSE_CODE.result();
        };
    }
}
