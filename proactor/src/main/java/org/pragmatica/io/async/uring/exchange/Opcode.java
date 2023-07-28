package org.pragmatica.io.async.uring.exchange;

public enum Opcode {
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

    Opcode(int opcode) {
        this.opcode = (byte) opcode;
    }

    public byte opcode() {
        return opcode;
    }
}
