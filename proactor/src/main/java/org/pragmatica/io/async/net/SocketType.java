/*
 *  Copyright (c) 2020-2022 Sergiy Yevtushenko.
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

package org.pragmatica.io.async.net;

/**
 * Socket types.
 */
public enum SocketType {
    STREAM(1),       // Provides sequenced, reliable, two-way, connection-based byte streams.
    DGRAM(2),        // Supports datagrams (connectionless, unreliable messages of a fixed maximum length).
    RAW(3),          // Provides raw network protocol access.
    RDM(4),          // Provides a reliable datagram layer that does not guarantee ordering.
    SEQPACKET(5);    // Provides a sequenced, reliable, two-way connection-based data transmission path for datagrams of fixed maximum length
    
    private final int code;

    SocketType(final int code) {
        this.code = code;
    }

    public int code() {
        return code;
    }
}
