/*
 * Copyright (c) 2020 Sergiy Yevtushenko
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.pfj.io.async.uring.exchange;

import org.pfj.io.async.Proactor;
import org.pfj.io.async.file.FileDescriptor;
import org.pfj.io.async.net.AddressFamily;
import org.pfj.io.async.net.SocketFlag;
import org.pfj.io.async.net.SocketOption;
import org.pfj.io.async.net.SocketType;
import org.pfj.io.async.uring.UringApi;
import org.pfj.io.async.uring.utils.PlainObjectPool;
import org.pfj.lang.Result;

import java.util.Set;
import java.util.function.BiConsumer;

import static org.pfj.io.async.uring.AsyncOperation.IORING_OP_NOP;

public class SocketExchangeEntry extends AbstractExchangeEntry<SocketExchangeEntry, FileDescriptor> {
    private AddressFamily addressFamily;
    private SocketType socketType;
    private Set<SocketFlag> openFlags;
    private Set<SocketOption> options;

    protected SocketExchangeEntry(PlainObjectPool<SocketExchangeEntry> pool) {
        super(IORING_OP_NOP, pool);
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
