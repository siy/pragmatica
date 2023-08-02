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

package org.pragmatica.protocol.http.parser.header;

import org.pragmatica.protocol.http.parser.util.Slice;

public interface HttpHeader {
    HeaderName name();
    Slice value();

    record SimpleHttpHeader(HeaderName name, Slice value) implements HttpHeader {
        @Override
        public String toString() {
            return "HttpHeader(\"" + name().canonicalName() + "\" = \"" + value.text() + "\")";
        }
    }

    static HttpHeader createParsed(HeaderName name, Slice value) {
        return new SimpleHttpHeader(name, value);
    }

    static HttpHeader fromString(HeaderName name, String value) {
        return new SimpleHttpHeader(name, Slice.fromString(value));
    }
}
