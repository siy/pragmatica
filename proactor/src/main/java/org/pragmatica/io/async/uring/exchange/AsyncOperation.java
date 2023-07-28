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

import org.pragmatica.io.async.SystemError;
import org.pragmatica.io.async.common.SizeT;
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.file.stat.FileStat;
import org.pragmatica.io.async.net.ConnectionContext;
import org.pragmatica.io.async.uring.Bitmask;
import org.pragmatica.io.async.uring.struct.raw.SQEntry;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.time.Duration;

import static java.time.temporal.ChronoUnit.NANOS;
import static org.pragmatica.io.async.net.ConnectionContext.connection;
import static org.pragmatica.lang.Result.success;
import static org.pragmatica.lang.Unit.unitResult;

/**
 * Asynchronous operation opcodes.
 */
public interface AsyncOperation<R> {
    Result<R> parseCompletion(ExchangeEntry<R> exchangeEntry, int result, int flags);
    SQEntry fillSubmissionEntry(ExchangeEntry<R> exchangeEntry, SQEntry sqEntry);

    AsyncOperation<Unit> NOP = new AsyncOperation<>() {
        @Override
        public Result<Unit> parseCompletion(ExchangeEntry<Unit> exchangeEntry, int result, int flags) {
            return unitResult();
        }

        @Override
        public SQEntry fillSubmissionEntry(ExchangeEntry<Unit> exchangeEntry, SQEntry sqEntry) {
            return sqEntry.userData(exchangeEntry.key())
                          .opcode(Opcode.NOP);
        }
    };

    AsyncOperation<SizeT> READV = new AsyncOperation<>() {
        @Override
        public Result<SizeT> parseCompletion(ExchangeEntry<SizeT> exchangeEntry, int result, int flags) {
            return ExchangeEntry.bytesReadToResult(result);
        }

        @Override
        public SQEntry fillSubmissionEntry(ExchangeEntry<SizeT> exchangeEntry, SQEntry sqEntry) {
            return sqEntry.userData(exchangeEntry.key())
                          .opcode(Opcode.READV)
                          .flags(exchangeEntry.flags())
                          .fd(exchangeEntry.descriptor().descriptor())
                          .addr(exchangeEntry.ioVector().address())
                          .len(exchangeEntry.ioVector().length())
                          .off(exchangeEntry.offset().value());
        }
    };

    AsyncOperation<SizeT> WRITEV = new AsyncOperation<>() {
        @Override
        public Result<SizeT> parseCompletion(ExchangeEntry<SizeT> exchangeEntry, int result, int flags) {
            return ExchangeEntry.byteCountToResult(result);
        }

        @Override
        public SQEntry fillSubmissionEntry(ExchangeEntry<SizeT> exchangeEntry, SQEntry sqEntry) {
            return sqEntry.userData(exchangeEntry.key())
                          .opcode(Opcode.WRITEV)
                          .flags(exchangeEntry.flags())
                          .fd(exchangeEntry.descriptor().descriptor())
                          .addr(exchangeEntry.ioVector().address())
                          .len(exchangeEntry.ioVector().length())
                          .off(exchangeEntry.offset().value());
        }
    };

    AsyncOperation<Unit> FSYNC = new AsyncOperation<>() {
        @Override
        public Result<Unit> parseCompletion(ExchangeEntry<Unit> exchangeEntry, int result, int flags) {
            return result == 0 ? unitResult() : SystemError.result(result);
        }

        @Override
        public SQEntry fillSubmissionEntry(ExchangeEntry<Unit> exchangeEntry, SQEntry sqEntry) {
            return sqEntry.userData(exchangeEntry.key())
                          .opcode(Opcode.FSYNC)
                          .flags(exchangeEntry.flags())
                          .syncFlags(exchangeEntry.syncFlags())
                          .fd(exchangeEntry.descriptor().descriptor());
        }
    };

    AsyncOperation<SizeT> READ_FIXED = new AsyncOperation<>() {
        @Override
        public Result<SizeT> parseCompletion(ExchangeEntry<SizeT> exchangeEntry, int result, int flags) {
            return ExchangeEntry.bytesReadToResult(result)
                                .onSuccess(exchangeEntry.fixedBuffer()::used);
        }

        @Override
        public SQEntry fillSubmissionEntry(ExchangeEntry<SizeT> exchangeEntry, SQEntry sqEntry) {
            return sqEntry.userData(exchangeEntry.key())
                          .opcode(Opcode.READ_FIXED)
                          .fd(exchangeEntry.descriptor().descriptor())
                          .flags(exchangeEntry.flags())
                          .addr(exchangeEntry.fixedBuffer().address())
                          .len(exchangeEntry.fixedBuffer().size())
                          .off(exchangeEntry.offset().value())
                          .bufIndex((short) 0);
        }
    };

    AsyncOperation<SizeT> WRITE_FIXED = new AsyncOperation<>() {
        @Override
        public Result<SizeT> parseCompletion(ExchangeEntry<SizeT> exchangeEntry, int result, int flags) {
            return ExchangeEntry.byteCountToResult(result);
        }

        @Override
        public SQEntry fillSubmissionEntry(ExchangeEntry<SizeT> exchangeEntry, SQEntry sqEntry) {
            return sqEntry.userData(exchangeEntry.key())
                          .opcode(Opcode.WRITE_FIXED)
                          .fd(exchangeEntry.descriptor().descriptor())
                          .flags(exchangeEntry.flags())
                          .addr(exchangeEntry.fixedBuffer().address())
                          .len(exchangeEntry.fixedBuffer().used())
                          .off(exchangeEntry.offset().value())
                          .bufIndex((short) 0);
        }
    };

    //    POLL_ADD(6),
//
//    POLL_REMOVE(7),
//
//    SYNC_FILE_RANGE(8),
//
//    SENDMSG(9),
//
//    RECVMSG(10),
//
    AsyncOperation<Duration> TIMEOUT = new AsyncOperation<>() {
        @Override
        public Result<Duration> parseCompletion(ExchangeEntry<Duration> exchangeEntry, int result, int flags) {
            return Math.abs(result) != SystemError.ETIME.code()
                   ? SystemError.result(result)
                   : success(Duration.of(System.nanoTime() - exchangeEntry.startNanos(), NANOS));

        }

        @Override
        public SQEntry fillSubmissionEntry(ExchangeEntry<Duration> exchangeEntry, SQEntry sqEntry) {
            exchangeEntry.startNanos(System.nanoTime());

            return sqEntry.userData(exchangeEntry.key())
                          .opcode(Opcode.TIMEOUT)
                          .addr(exchangeEntry.delayTime().address())
                          .fd(-1)
                          .len(1)
                          .off(1);
        }
    };
    //    TIMEOUT_REMOVE(12),
    AsyncOperation<ConnectionContext<?>> ACCEPT = new AsyncOperation<>() {
        @Override
        public Result<ConnectionContext<?>> parseCompletion(ExchangeEntry<ConnectionContext<?>> exchangeEntry, int result, int flags) {
            return result <= 0
                   ? SystemError.result(result)
                   : exchangeEntry.remoteAddress().extract()
                                  .map(address -> connection(result, address));
        }

        @Override
        public SQEntry fillSubmissionEntry(ExchangeEntry<ConnectionContext<?>> exchangeEntry, SQEntry sqEntry) {
            return sqEntry.userData(exchangeEntry.key())
                          .opcode(Opcode.ACCEPT)
                          .fd(exchangeEntry.descriptor().descriptor())
                          .addr(exchangeEntry.remoteAddress().sockAddrPtr())
                          .off(exchangeEntry.remoteAddress().sizePtr())
                          .acceptFlags(exchangeEntry.acceptFlags());
        }
    };
    //    ASYNC_CANCEL(14),
    AsyncOperation<Unit> LINK_TIMEOUT = new AsyncOperation<>() {
        @Override
        public Result<Unit> parseCompletion(ExchangeEntry<Unit> exchangeEntry, int result, int flags) {
            throw new UnsupportedOperationException("LINK_TIMEOUT is not an actual operation and should not be handled as such");
        }

        @Override
        public SQEntry fillSubmissionEntry(ExchangeEntry<Unit> exchangeEntry, SQEntry sqEntry) {
            return sqEntry.userData(exchangeEntry.key())
                          .opcode(Opcode.LINK_TIMEOUT)
                          .addr(exchangeEntry.operationTimeout().address())
                          .fd(-1)
                          .len(1);
        }
    };
    AsyncOperation<FileDescriptor> CONNECT = new AsyncOperation<>() {
        @Override
        public Result<FileDescriptor> parseCompletion(ExchangeEntry<FileDescriptor> exchangeEntry, int result, int flags) {
            return result < 0
                   ? SystemError.result(result)
                   : success(exchangeEntry.descriptor());

        }

        @Override
        public SQEntry fillSubmissionEntry(ExchangeEntry<FileDescriptor> exchangeEntry, SQEntry sqEntry) {
            return sqEntry.userData(exchangeEntry.key())
                          .opcode(Opcode.CONNECT)
                          .fd(exchangeEntry.descriptor().descriptor())
                          .flags(exchangeEntry.flags())
                          .addr(exchangeEntry.destinationAddress().sockAddrPtr())
                          .off(exchangeEntry.destinationAddress().sockAddrSize());
        }
    };
    AsyncOperation<Unit> FALLOCATE = new AsyncOperation<>() {
        @Override
        public Result<Unit> parseCompletion(ExchangeEntry<Unit> exchangeEntry, int result, int flags) {
            return result == 0
                   ? unitResult()
                   : SystemError.result(result);
        }

        @Override
        public SQEntry fillSubmissionEntry(ExchangeEntry<Unit> exchangeEntry, SQEntry sqEntry) {
            return sqEntry.userData(exchangeEntry.key())
                          .opcode(Opcode.FALLOCATE)
                          .flags(exchangeEntry.flags())
                          .addr(exchangeEntry.len())
                          .len(exchangeEntry.allocFlags())
                          .off(exchangeEntry.offset().value())
                          .fd(exchangeEntry.descriptor().descriptor());
        }
    };
    //
    AsyncOperation<FileDescriptor> OPENAT = new AsyncOperation<>() {
        private static final int AT_FDCWD = -100;

        @Override
        public Result<FileDescriptor> parseCompletion(ExchangeEntry<FileDescriptor> exchangeEntry, int result, int flags) {
            return result < 0
                   ? SystemError.result(result)
                   : success(FileDescriptor.file(result));
        }

        @Override
        public SQEntry fillSubmissionEntry(ExchangeEntry<FileDescriptor> exchangeEntry, SQEntry sqEntry) {
            return sqEntry.userData(exchangeEntry.key())
                          .opcode(Opcode.OPENAT)
                          .flags(exchangeEntry.flags())
                          .fd(AT_FDCWD)
                          .addr(exchangeEntry.rawPath().address())
                          .len(exchangeEntry.openMode())
                          .openFlags(exchangeEntry.openFlags());

        }
    };
    AsyncOperation<Unit> CLOSE = new AsyncOperation<>() {
        @Override
        public Result<Unit> parseCompletion(ExchangeEntry<Unit> exchangeEntry, int result, int flags) {
            return result == 0 ? unitResult() : SystemError.result(result);
        }

        @Override
        public SQEntry fillSubmissionEntry(ExchangeEntry<Unit> exchangeEntry, SQEntry sqEntry) {
            return sqEntry.userData(exchangeEntry.key())
                          .opcode(Opcode.CLOSE)
                          .flags(exchangeEntry.flags())
                          .fd(exchangeEntry.descriptor().descriptor());
        }
    };
    //    FILES_UPDATE(20),
//
    AsyncOperation<FileStat> STATX = new AsyncOperation<>() {
        @Override
        public Result<FileStat> parseCompletion(ExchangeEntry<FileStat> exchangeEntry, int result, int flags) {
            return result < 0
                   ? SystemError.result(result)
                   : success(exchangeEntry.fileStat().extract());
        }

        @Override
        public SQEntry fillSubmissionEntry(ExchangeEntry<FileStat> exchangeEntry, SQEntry sqEntry) {
            exchangeEntry.fileStat().clear();

            return sqEntry.userData(exchangeEntry.key())
                          .opcode(Opcode.STATX)
                          .fd(exchangeEntry.descriptor().descriptor())
                          .addr(exchangeEntry.rawPath().address())
                          .len(exchangeEntry.statMask())
                          .off(exchangeEntry.fileStat().address())
                          .statxFlags(exchangeEntry.statFlags());

        }
    };
    AsyncOperation<SizeT> READ = new AsyncOperation<>() {
        @Override
        public Result<SizeT> parseCompletion(ExchangeEntry<SizeT> exchangeEntry, int result, int flags) {
            return ExchangeEntry.bytesReadToResult(result)
                                .onSuccess(exchangeEntry.buffer()::used);
        }

        @Override
        public SQEntry fillSubmissionEntry(ExchangeEntry<SizeT> exchangeEntry, SQEntry sqEntry) {
            return sqEntry.userData(exchangeEntry.key())
                          .opcode(Opcode.READ)
                          .fd(exchangeEntry.descriptor().descriptor())
                          .flags(exchangeEntry.flags())
                          .addr(exchangeEntry.buffer().address())
                          .len(exchangeEntry.buffer().size())
                          .off(exchangeEntry.offset().value());
        }
    };
    //
    AsyncOperation<SizeT> WRITE = new AsyncOperation<>() {
        @Override
        public Result<SizeT> parseCompletion(ExchangeEntry<SizeT> exchangeEntry, int result, int flags) {
            return ExchangeEntry.byteCountToResult(result);
        }

        @Override
        public SQEntry fillSubmissionEntry(ExchangeEntry<SizeT> exchangeEntry, SQEntry sqEntry) {
            return sqEntry.userData(exchangeEntry.key())
                          .opcode(Opcode.WRITE)
                          .fd(exchangeEntry.descriptor().descriptor())
                          .flags(exchangeEntry.flags())
                          .addr(exchangeEntry.buffer().address())
                          .len(exchangeEntry.buffer().used())
                          .off(exchangeEntry.offset().value());

        }
    };
    //
//    FADVISE(24),
//
//    MADVISE(25),
    AsyncOperation<SizeT> SEND = new AsyncOperation<>() {
        @Override
        public Result<SizeT> parseCompletion(ExchangeEntry<SizeT> exchangeEntry, int result, int flags) {
            return ExchangeEntry.byteCountToResult(result);
        }

        @Override
        public SQEntry fillSubmissionEntry(ExchangeEntry<SizeT> exchangeEntry, SQEntry sqEntry) {
            return sqEntry.userData(exchangeEntry.key())
                          .opcode(Opcode.SEND)
                          .fd(exchangeEntry.descriptor().descriptor())
                          .flags(exchangeEntry.flags())
                          .msgFlags(exchangeEntry.msgFlags())
                          .addr(exchangeEntry.buffer().address())
                          .len(exchangeEntry.buffer().used());

        }
    };
    AsyncOperation<SizeT> RECV = new AsyncOperation<>() {
        @Override
        public Result<SizeT> parseCompletion(ExchangeEntry<SizeT> exchangeEntry, int result, int flags) {
            return ExchangeEntry.bytesReadToResult(result)
                                .onSuccess(exchangeEntry.buffer()::used);
        }

        @Override
        public SQEntry fillSubmissionEntry(ExchangeEntry<SizeT> exchangeEntry, SQEntry sqEntry) {
            return sqEntry.userData(exchangeEntry.key())
                          .opcode(Opcode.RECV)
                          .fd(exchangeEntry.descriptor().descriptor())
                          .flags(exchangeEntry.flags())
                          .msgFlags(exchangeEntry.msgFlags())
                          .addr(exchangeEntry.buffer().address())
                          .len(exchangeEntry.buffer().size());

        }
    };
    //
//    OPENAT2(28),
//
//    EPOLL_CTL(29),
//
    AsyncOperation<SizeT> SPLICE = new AsyncOperation<>() {
        @Override
        public Result<SizeT> parseCompletion(ExchangeEntry<SizeT> exchangeEntry, int result, int flags) {
            return ExchangeEntry.byteCountToResult(result);
        }

        @Override
        public SQEntry fillSubmissionEntry(ExchangeEntry<SizeT> exchangeEntry, SQEntry sqEntry) {
            return sqEntry.userData(exchangeEntry.key())
                          .opcode(Opcode.SPLICE)
                          .flags(exchangeEntry.flags())
                          .fd(exchangeEntry.spliceDescriptor().toDescriptor().descriptor())
                          .len((int) exchangeEntry.spliceDescriptor().bytesToCopy().value())
                          .off(exchangeEntry.spliceDescriptor().toOffset().value())
                          .spliceFdIn(exchangeEntry.spliceDescriptor().fromDescriptor().descriptor())
                          .spliceOffIn(exchangeEntry.spliceDescriptor().fromOffset().value())
                          .spliceFlags(Bitmask.combine(exchangeEntry.spliceDescriptor().flags()));
        }
    };
//
//    PROVIDE_BUFFERS(31),
//
//    REMOVE_BUFFERS(32),
//
//    TEE(33),
//
//    SHUTDOWN(34),
//
//    RENAMEAT(35),
//
//    UNLINKAT(36),
//
//    MKDIRAT(37),
//
//    SYMLINKAT(38),
//
//    LINKAT(39);
}
