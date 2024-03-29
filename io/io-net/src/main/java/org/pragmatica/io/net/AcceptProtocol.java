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

import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.net.InetAddress;

/**
 * Protocol which handles accepting of the connection requests.
 */
public interface AcceptProtocol<T extends InetAddress> {
    /**
     * Process successful connection accept.
     * <p>
     * WARNING: Provided {@link Proactor} instance is transient, it should not be stored nor used outside the method body.
     *
     * @param context  connection context
     */
    void accept(ConnectionProtocolContext<T> context);
}
