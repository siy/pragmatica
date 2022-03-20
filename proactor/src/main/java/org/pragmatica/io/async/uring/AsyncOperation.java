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

package org.pragmatica.io.async.uring;

/**
 * Asynchronous operation opcodes.
 */
public enum AsyncOperation {
    NOP(0),                  //Implemented
    READV(1),                //Implemented
    WRITEV(2),               //Implemented
    FSYNC(3),                //Implemented
    READ_FIXED(4),           //Implemented
    WRITE_FIXED(5),          //Implemented
    POLL_ADD(6),
    POLL_REMOVE(7),
    SYNC_FILE_RANGE(8),
    SENDMSG(9),
    RECVMSG(10),
    TIMEOUT(11),              //Implemented
    TIMEOUT_REMOVE(12),
    ACCEPT(13),               //Implemented
    ASYNC_CANCEL(14),
    LINK_TIMEOUT(15),         //Implemented
    CONNECT(16),              //Implemented
    FALLOCATE(17),            //Implemented
    OPENAT(18),               //Implemented
    CLOSE(19),                //Implemented
    FILES_UPDATE(20),
    STATX(21),                //Implemented
    READ(22),                 //Implemented
    WRITE(23),                //Implemented
    FADVISE(24),
    MADVISE(25),
    SEND(26),
    RECV(27),
    OPENAT2(28),
    EPOLL_CTL(29),
    SPLICE(30),               //Implemented
    PROVIDE_BUFFERS(31),
    REMOVE_BUFFERS(32),
    TEE(33),
    SHUTDOWN(34),
    RENAMEAT(35),
    UNLINKAT(36),
    MKDIRAT(37),
    SYMLINKAT(38),
    LINKAT(39);

    private final byte opcode;

    AsyncOperation(int opcode) {
        this.opcode = (byte) opcode;
    }

    public byte opcode() {
        return opcode;
    }
}
