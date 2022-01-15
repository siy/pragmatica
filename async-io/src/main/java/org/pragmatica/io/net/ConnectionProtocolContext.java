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

package org.pragmatica.io.net;

import org.pragmatica.io.async.net.ConnectionContext;
import org.pragmatica.io.async.net.InetAddress;
import org.pragmatica.io.async.net.ListenContext;

/**
 * Context provided to the {@link ConnectionProtocolStarter}.
 */
public interface ConnectionProtocolContext<T extends InetAddress> {
    ListenContext<T> listenContext();

    ConnectionContext<T> connectionContext();

    static <T extends InetAddress> ConnectionProtocolContext<T> connectionProtocolContext(ListenContext<T> listenContext,
                                                                                          ConnectionContext<T> connectionContext) {
        record connectionProtocolContext<T extends InetAddress>(ListenContext<T> listenContext,
                                            ConnectionContext<T> connectionContext)
        implements ConnectionProtocolContext<T> {};

        return new connectionProtocolContext<>(listenContext, connectionContext);
    }
}
