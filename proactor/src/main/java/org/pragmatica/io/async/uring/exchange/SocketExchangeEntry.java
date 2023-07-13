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
 */

package org.pragmatica.io.async.uring.exchange;

import org.pragmatica.io.async.Proactor;
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.net.AddressFamily;
import org.pragmatica.io.async.net.SocketFlag;
import org.pragmatica.io.async.net.SocketOption;
import org.pragmatica.io.async.net.SocketType;
import org.pragmatica.io.async.uring.UringApi;
import org.pragmatica.io.async.uring.utils.PlainObjectPool;
import org.pragmatica.lang.Result;

import java.util.Set;
import java.util.function.BiConsumer;

import static org.pragmatica.io.async.uring.AsyncOperation.NOP;

/**
 * Exchange entry for {@code socket} request.
 */
public class SocketExchangeEntry extends AbstractExchangeEntry<SocketExchangeEntry, FileDescriptor> {
    private AddressFamily addressFamily;
    private SocketType socketType;
    private Set<SocketFlag> openFlags;
    private Set<SocketOption> options;

    protected SocketExchangeEntry(PlainObjectPool<SocketExchangeEntry> pool) {
        super(NOP, pool);
    }

    @Override
    protected void doAccept(int result, int flags, Proactor proactor) {
        completion.accept(UringApi.socket(addressFamily, socketType, openFlags, options),
                          proactor);
    }

    public SocketExchangeEntry prepare(BiConsumer<Result<FileDescriptor>, Proactor> completion,
                                       AddressFamily addressFamily,
                                       SocketType socketType,
                                       Set<SocketFlag> openFlags,
                                       Set<SocketOption> options) {
        this.addressFamily = addressFamily;
        this.socketType = socketType;
        this.openFlags = openFlags;
        this.options = options;
        return super.prepare(completion);
    }
}
