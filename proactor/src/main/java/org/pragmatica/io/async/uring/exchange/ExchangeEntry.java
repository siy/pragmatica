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
import org.pragmatica.io.async.SystemError;
import org.pragmatica.io.async.Timeout;
import org.pragmatica.io.async.common.OffsetT;
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.file.SpliceDescriptor;
import org.pragmatica.io.async.net.ProtocolVersion;
import org.pragmatica.io.async.uring.struct.offheap.*;
import org.pragmatica.io.async.uring.struct.raw.CQEntry;
import org.pragmatica.io.async.uring.struct.raw.SQEntry;
import org.pragmatica.io.async.uring.struct.raw.SQEntryFlags;
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.io.async.util.allocator.FixedBuffer;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.util.function.BiConsumer;

import static org.pragmatica.io.async.common.SizeT.sizeT;
import static org.pragmatica.lang.Result.success;

/**
 * Internal representation of in-flight IO request.
 */
public class ExchangeEntry<R> {
    private final int key;
    private final OffHeapSocketAddress remoteAddress = OffHeapSocketAddress.v4();
    private final OffHeapTimeSpec delayTime = OffHeapTimeSpec.uninitialized();
    private final OffHeapFileStat fileStat = OffHeapFileStat.fileStat();
    private final OffHeapTimeSpec operationTimeout = OffHeapTimeSpec.uninitialized();

    private FixedBuffer fixedBuffer;
    private OffHeapSlice buffer;
    private OffHeapSocketAddress destinationAddress;
    private OffHeapCString rawPath;
    private OffHeapIoVector ioVector;
    private OffsetT offset;
    private FileDescriptor descriptor;
    private SpliceDescriptor spliceDescriptor;

    private AsyncOperation<R> operation;
    private BiConsumer<Result<R>, Proactor> completion;
    private long startNanos;
    private long len;
    private int openMode;
    private byte flags;
    private int fsyncFlags;
    private int acceptFlags;
    private int allocFlags;
    private int openFlags;
    private int msgFlags;
    private int statFlags;
    private int statMask;

    private ExchangeEntry(final int key) {
        this.key = key;
    }

    public static <R> ExchangeEntry<R> exchangeEntry(final int key) {
        return new ExchangeEntry<>(key);
    }

    public BiConsumer<Result<R>, Proactor> completion() {
        return completion;
    }

    private void cleanup() {
        operation = null;
        completion = null;
        fixedBuffer = null;
        buffer = null;
        destinationAddress = null;
        spliceDescriptor = null;
        descriptor = null;
        offset = null;

        if (ioVector != null) {
            ioVector.dispose();
            ioVector = null;
        }

        if (rawPath != null) {
            rawPath.dispose();
            rawPath = null;
        }
    }

    public void close() {
        cleanup();
        operationTimeout.dispose();
        remoteAddress.dispose();
        delayTime.dispose();
        fileStat.dispose();
    }

    public int key() {
        return key;
    }

    public AsyncOperation<R> operation() {
        return operation;
    }

    public ExchangeEntry<R> operation(AsyncOperation<R> operation) {
        this.operation = operation;
        return this;
    }

    public SQEntry fill(SQEntry entry) {
        entry.headPad(0L) // 0-7
             .lenPad(0L)  // 24-31
             .bufPad(0L); // 40-47

        return operation().fillSubmissionEntry(this, entry);
    }

    //TODO: test timeouts
    @SuppressWarnings("unchecked")
    public void fillTimeout(SQEntry entry) {
        entry.headPad(0L) // 0-7
             .lenPad(0L)  // 24-31
             .bufPad(0L); // 40-47
        AsyncOperation.LINK_TIMEOUT.fillSubmissionEntry((ExchangeEntry<Unit>) this, entry);
    }

    public ExchangeEntry<R> processCompletion(CQEntry entry, Proactor proactor) {
        completion().accept(parseCompletionOutcome(entry.res(), entry.flags()), proactor);
        cleanup();
        return this;
    }

    public Result<R> parseCompletionOutcome(int res, int flags) {
        return operation().parseCompletion(this, res, flags);
    }

    public ExchangeEntry<R> completion(BiConsumer<Result<R>, Proactor> completion) {
        this.completion = completion;
        return this;
    }

    public OffHeapIoVector ioVector() {
        return ioVector;
    }

    public ExchangeEntry<R> ioVector(OffHeapIoVector ioVector) {
        this.ioVector = ioVector;
        return this;
    }

    public FixedBuffer fixedBuffer() {
        return fixedBuffer;
    }

    public ExchangeEntry<R> fixedBuffer(FixedBuffer buffer) {
        this.fixedBuffer = buffer;
        return this;
    }

    public OffsetT offset() {
        return offset;
    }

    public ExchangeEntry<R> offset(OffsetT offset) {
        this.offset = offset;
        return this;
    }

    public int syncFlags() {
        return fsyncFlags;
    }

    public ExchangeEntry<R> syncFlags(int fsyncFlags) {
        this.fsyncFlags = fsyncFlags;
        return this;
    }

    public ExchangeEntry<R> protocolVersion(ProtocolVersion version) {
        remoteAddress().protocolVersion(version);
        return this;
    }

    public OffHeapSocketAddress remoteAddress() {
        return remoteAddress;
    }

    public FileDescriptor descriptor() {
        return descriptor;
    }

    public ExchangeEntry<R> descriptor(FileDescriptor descriptor) {
        this.descriptor = descriptor;
        return this;
    }

    public byte flags() {
        return flags;
    }

    public int acceptFlags() {
        return acceptFlags;
    }

    public ExchangeEntry<R> acceptFlags(int acceptFlags) {
        this.acceptFlags = acceptFlags;
        return this;
    }

    public OffHeapSocketAddress destinationAddress() {
        return destinationAddress;
    }

    public ExchangeEntry<R> destinationAddress(OffHeapSocketAddress destinationAddress) {
        this.destinationAddress = destinationAddress;
        return this;
    }

    public OffHeapTimeSpec delayTime() {
        return delayTime;
    }

    public ExchangeEntry<R> setDelayTime(Timeout delayTime) {
        this.delayTime.set(delayTime);
        return this;
    }

    public long startNanos() {
        return startNanos;
    }

    public ExchangeEntry<R> startNanos(long startNanos) {
        this.startNanos = startNanos;
        return this;
    }

    public int allocFlags() {
        return allocFlags;
    }

    public ExchangeEntry<R> allocFlags(int allocFlags) {
        this.allocFlags = allocFlags;
        return this;
    }

    public long len() {
        return len;
    }

    public ExchangeEntry<R> len(long len) {
        this.len = len;
        return this;
    }

    public OffHeapCString rawPath() {
        return rawPath;
    }

    public ExchangeEntry<R> rawPath(OffHeapCString rawPath) {
        this.rawPath = rawPath;
        return this;
    }

    public int openFlags() {
        return openFlags;
    }

    public ExchangeEntry<R> openFlags(int openFlags) {
        this.openFlags = openFlags;
        return this;
    }

    public int openMode() {
        return openMode;
    }

    public ExchangeEntry<R> openMode(int mode) {
        this.openMode = mode;
        return this;
    }

    public OffHeapSlice buffer() {
        return buffer;
    }

    public ExchangeEntry<R> buffer(OffHeapSlice buffer) {
        this.buffer = buffer;
        return this;
    }

    public int msgFlags() {
        return msgFlags;
    }

    public ExchangeEntry<R> msgFlags(int msgFlags) {
        this.msgFlags = msgFlags;
        return this;
    }

    public SpliceDescriptor spliceDescriptor() {
        return spliceDescriptor;
    }

    public ExchangeEntry<R> spliceDescriptor(SpliceDescriptor spliceDescriptor) {
        this.spliceDescriptor = spliceDescriptor;
        return this;
    }

    public OffHeapFileStat fileStat() {
        return fileStat;
    }

    public int statFlags() {
        return statFlags;
    }

    public ExchangeEntry<R> statFlags(int statFlags) {
        this.statFlags = statFlags;
        return this;
    }

    public int statMask() {
        return statMask;
    }

    public ExchangeEntry<R> statMask(int statMask) {
        this.statMask = statMask;
        return this;
    }

    public ExchangeEntry<R> setOperationTimeout(Option<Timeout> timeout) {
        timeout.onPresent(operationTimeout::set);
        flags = timeout.isEmpty()
                ? SQEntryFlags.NONE.byteMask()
                : SQEntryFlags.IO_LINK.byteMask();
        return this;
    }

    public OffHeapTimeSpec operationTimeout() {
        return operationTimeout;
    }

    public boolean hasTimeout() {
        return (flags & SQEntryFlags.IO_LINK.mask()) != 0;
    }

    private static final int RESULT_SIZET_POOL_SIZE = 65536;
    @SuppressWarnings("rawtypes")
    private static final Result[] RESULT_SIZET_POOL;
    private static final Result<SizeT> EOF_RESULT = SystemError.ENODATA.result();

    static {
        RESULT_SIZET_POOL = new Result[RESULT_SIZET_POOL_SIZE];

        for (int i = 0; i < RESULT_SIZET_POOL.length; i++) {
            RESULT_SIZET_POOL[i] = success(sizeT(i));
        }
    }

    protected static Result<SizeT> byteCountToResult(int res) {
        return res > 0
               ? sizeResult(res)
               : SystemError.result(res);
    }

    protected static Result<SizeT> bytesReadToResult(int res) {
        return res == 0 ? EOF_RESULT
                        : res > 0 ? sizeResult(res)
                                  : SystemError.result(res);
    }

    @SuppressWarnings("unchecked")
    protected static Result<SizeT> sizeResult(int res) {
        return res < RESULT_SIZET_POOL.length
               ? RESULT_SIZET_POOL[res]
               : success(sizeT(res));
    }
}
