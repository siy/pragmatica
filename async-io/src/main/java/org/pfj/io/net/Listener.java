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

package org.pfj.io.net;

import org.pfj.io.async.net.InetAddress;
import org.pfj.io.net.tcp.ListenConfig;
import org.pfj.io.net.tcp.TcpListener;
import org.pfj.lang.Promise;
import org.pfj.lang.Unit;

import java.util.function.Supplier;

/**
 * Incoming connection listener.
 */
public interface Listener<T extends InetAddress> {
    Promise<Unit> listen(AcceptProtocol<T> protocol);

    Promise<Unit> shutdown();

    default Promise<Unit> shutdown(Unit unit) {
        return shutdown();
    }

    Thread shutdownHook();

    static <T extends InetAddress> Listener<T> listener(ListenConfig<T> config) {
        return TcpListener.tcpListener(config);
    }
}
