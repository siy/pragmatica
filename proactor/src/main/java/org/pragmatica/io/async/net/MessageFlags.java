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

import org.pragmatica.io.async.uring.Bitmask;

/**
 * Flags for send/recv, sendto/recvfrom, sendmsg/recvmsg.
 */
public enum MessageFlags implements Bitmask {
    OOB(0x01),              /* Process out-of-band data.  */
    PEEK(0x02),             /* Peek at incoming messages.  */
    DONTROUTE(0x04),        /* Don't use local routing.  */
    CTRUNC(0x08),           /* Control data lost before delivery.  */
    PROXY(0x10),            /* Supply or ask second address.  */
    TRUNC(0x20),
    DONTWAIT(0x40),         /* Nonblocking IO.  */
    EOR(0x80),              /* End of record.  */
    WAITALL(0x100),         /* Wait for a full request.  */
    FIN(0x200),
    SYN(0x400),
    CONFIRM(0x800),         /* Confirm path validity.  */
    RST(0x1000),
    ERRQUEUE(0x2000),       /* Fetch message from error queue.  */
    NOSIGNAL(0x4000),       /* Do not generate SIGPIPE.  */
    MORE(0x8000),           /* Sender will send more.  */
    WAITFORONE(0x1_0000),   /* Wait for at least one packet to return.*/
    BATCH(0x4_0000),        /* sendmmsg: more messages coming.  */
    ZEROCOPY(0x400_0000),   /* Use user data in kernel path.  */
    FASTOPEN(0x2000_0000),  /* Send data in TCP SYN.  */
    CMSG_CLOEXEC(0x4000_0000);   /* Set close_on_exit for file descriptor received through SCM_RIGHTS.  */

    private final int mask;

    MessageFlags(int mask) {

        this.mask = mask;
    }

    @Override
    public int mask() {
        return mask;
    }
}
