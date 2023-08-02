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

package org.pragmatica.protocol.http.parser;

import org.pragmatica.lang.Result;

public enum ParsingErrors implements Result.Cause {
    UNKNOWN_METHOD("Unknown method"),
    REQUEST_HEADER_TOO_LONG("Request header length exceeds limit"),
    INVALID_REQUEST_HEADER("Invalid request header"),
    INVALID_URI("Invalid URI"),
    INVALID_CHARACTER_IN_HEADER("Invalid character in header"),
    INVALID_REQUEST_HEADER_NAME("Invalid header name"),
    INVALID_REQUEST_HEADER_VALUE("Invalid header value"),
    INVALID_HTTP_VERSION("Invalid HTTP protocol version"),
    INVALID_STATUS_CODE("Invalid HTTP status code"),
    ;

    private final String message;
    private final Result<?> result;

    ParsingErrors(String message) {
        this.message = message;
        this.result = Result.failure(this);
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
