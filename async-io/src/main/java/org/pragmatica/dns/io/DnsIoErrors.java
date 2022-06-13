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

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Result;

public enum DnsIoErrors implements Cause {
    TOO_SHORT_INPUT("Input does not contain full DNS response message"),
    INVALID_OPCODE("Invalid operation code"),
    INVALID_RESPONSE_CODE("Invalid response code"),
    INVALID_RECORD_TYPE("Invalid record type"),
    INVALID_RECORD_CLASS("Invalid record class"),
    INVALID_MESSAGE_TYPE("Invalid message type"),
    NO_RESULTS_FOUND("No results found")
    ;

    private final Result<?> result;
    private final String message;

    DnsIoErrors(String message) {
        this.message = message;
        this.result = Cause.super.result();
    }

    @Override
    public String message() {
        return message;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> Result<T> result() {
        return (Result<T>) result;
    }
}
