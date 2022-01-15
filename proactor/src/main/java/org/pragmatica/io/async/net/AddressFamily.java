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

package org.pragmatica.io.async.net;

import org.pragmatica.lang.Result;

import static org.pragmatica.io.async.SystemError.EPFNOSUPPORT;
import static org.pragmatica.lang.Option.option;

/**
 * Protocol address families (domains) which can be used for communication.
 */
public enum AddressFamily {
    UNIX(1),           // Local communication
    LOCAL(1),          // Synonym for AF_UNIX
    INET(2),           // IPv4 Internet protocols
    AX25(3),           // Amateur radio AX.25 protocol
    IPX(4),            // IPX - Novell protocols
    APPLETALK(5),      // AppleTalk
    X25(9),            // ITU-T X.25 / ISO-8208 protocol
    INET6(10),         // IPv6 Internet protocols
    DEC_NET(12),       // DECet protocol sockets
    KEY(15),           // Key management protocol, originally developed for usage with IPsec
    NETLINK(16),       // Kernel user interface device
    PACKET(17),        // Low-level packet interface
    RDS(21),           // Reliable Datagram Sockets (RDS) protocol
    PPPOX(24),         // Generic PPP transport layer, for setting up L2 tunnels (L2TP and PPPoE)
    LLC(26),           // Logical link control (IEEE 802.2 LLC) protocol
    IB(27),            // InfiniBand native addressing
    MPLS(28),          // Multiprotocol Label Switching
    CAN(29),           // Controller Area Network automotive bus protocol
    TIPC(30),          // TIPC, cluster domain sockets protocol
    BLUETOOTH(31),     // Bluetooth low-level socket protocol
    ALG(38),           // Interface to kernel crypto API
    VSOCK(40),         // VSOCK protocol for hypervisor-guest communication
    KCM(41),           // KCM (kernel connection multiplexor) interface
    XDP(44),           // XDP (express data path) interface
    ;

    private final short id;
    private static final AddressFamily[] values = AddressFamily.values();

    AddressFamily(final int id) {
        this.id = (short) id;
    }

    public static AddressFamily unsafeFromCode(final short family) {
        int low = 0;
        int high = values.length - 1;

        while (low <= high) {
            final int mid = (low + high) >>> 1;

            final int cmp = values[mid].id - family;
            if (cmp < 0) {
                low = mid + 1;
            } else if (cmp > 0) {
                high = mid - 1;
            } else {
                return values[mid];
            }
        }

        return null;
    }

    public static Result<AddressFamily> addressFamily(final short family) {
        return option(unsafeFromCode(family))
            .toResult(EPFNOSUPPORT);
    }

    public short familyId() {
        return id;
    }
}
