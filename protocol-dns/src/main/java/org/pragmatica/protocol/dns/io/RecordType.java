/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.pragmatica.protocol.dns.io;


import org.pragmatica.io.async.net.InetAddress.Inet4Address;
import org.pragmatica.io.async.util.SliceAccessor;
import org.pragmatica.protocol.dns.ResourceRecord;

import java.util.Arrays;
import java.util.Comparator;

import static org.pragmatica.io.async.net.InetAddress.inet4Address;
import static org.pragmatica.io.async.net.InetAddress.inet6Address;
import static org.pragmatica.protocol.dns.io.Decoding.decodeDomainName;
import static org.pragmatica.protocol.dns.io.Encoding.*;

public enum RecordType implements RecordEncoder, RecordDecoder {
    A(1, true) {
        @Override
        public AttributeBuilder decode(SliceAccessor sliceAccessor, AttributeBuilder attributeBuilder, int length) {
            return attributeBuilder.ip(inet4Address(sliceAccessor.getBytes(length)));
        }

        @Override
        public void encode(SliceAccessor buffer, ResourceRecord record) {
            encodeRecord(buffer, record);
        }

        @Override
        public void encodeData(SliceAccessor buffer, ResourceRecord record) {
            var ipAddress = record.domainData().ip();

            if (ipAddress instanceof Inet4Address ipV4Address) {
                encodeIp4Address(buffer, ipV4Address);
            } else {
                throw new IllegalArgumentException("Invalid address + " + ipAddress);
            }
        }
    },
    NS(2, true) {
        @Override
        public AttributeBuilder decode(SliceAccessor sliceAccessor, AttributeBuilder attributeBuilder, int length) {
            return attributeBuilder.domainName(decodeDomainName(sliceAccessor));
        }

        @Override
        public void encode(SliceAccessor buffer, ResourceRecord record) {
            encodeRecord(buffer, record);
        }

        @Override
        public void encodeData(SliceAccessor buffer, ResourceRecord record) {
            encodeDomainName(buffer, record.domainName());
        }
    },

    MD(3, true),
    MF(4, true),
    CNAME(5, true) {
        @Override
        public void encode(SliceAccessor buffer, ResourceRecord record) {
            encodeRecord(buffer, record);
        }

        @Override
        public void encodeData(SliceAccessor buffer, ResourceRecord record) {
            encodeDomainName(buffer, record.domainName());
        }
    },
    SOA(6, true) {
        @Override
        public void encode(SliceAccessor buffer, ResourceRecord record) {
            encodeRecord(buffer, record);
        }

        @Override
        public void encodeData(SliceAccessor buffer, ResourceRecord record) {
            encodeDomainName(buffer, record.soaData().mName());
            encodeDomainName(buffer, record.soaData().rName());

            buffer.putIntInNetOrder((int) record.soaData().serial());
            buffer.putIntInNetOrder(record.soaData().refresh());
            buffer.putIntInNetOrder(record.soaData().retry());
            buffer.putIntInNetOrder(record.soaData().expire());
            buffer.putIntInNetOrder((int) record.soaData().minimum());
        }
    },
    MB(7, true),
    MG(8, true),
    MR(9, true),
    NULL(10, true),
    WKS(11, true),
    PTR(12, true) {
        @Override
        public void encode(SliceAccessor buffer, ResourceRecord record) {
            encodeRecord(buffer, record);
        }

        @Override
        public void encodeData(SliceAccessor buffer, ResourceRecord record) {
            encodeDomainName(buffer, record.domainName());
        }
    },
    HINFO(13, true),
    MINFO(14, true),
    MX(15, true) {
        @Override
        public AttributeBuilder decode(SliceAccessor sliceAccessor, AttributeBuilder attributeBuilder, int length) {
            return attributeBuilder
                .mxReference(sliceAccessor.getShortInNetOrder())
                .domainName(decodeDomainName(sliceAccessor));
        }

        @Override
        public void encode(SliceAccessor buffer, ResourceRecord record) {
            encodeRecord(buffer, record);
        }

        @Override
        public void encodeData(SliceAccessor buffer, ResourceRecord record) {
            buffer.putShortInNetOrder(record.domainData().mxReference());
            encodeDomainName(buffer, record.domainName());
        }
    },
    TXT(16, true) {
        @Override
        public void encode(SliceAccessor buffer, ResourceRecord record) {
            encodeRecord(buffer, record);
        }

        @Override
        public void encodeData(SliceAccessor buffer, ResourceRecord record) {
            encodeCharacterString(buffer, record.domainData().characterString());
        }
    },
    RP(17, true),
    AFSDB(18, true),
    X25(19, true),
    ISDN(20, true),
    RT(21, true),
    NSAP(22, true),
    NSAP_PTR(23, true),
    SIG(24, true),
    KEY(25, true),
    PX(26, true),
    GPOS(27, true),
    AAAA(28, true) {
        @Override
        public AttributeBuilder decode(SliceAccessor sliceAccessor, AttributeBuilder attributeBuilder, int length) {
            return attributeBuilder.ip(inet6Address(sliceAccessor.getBytes(length)));
        }
    },
    LOC(29, true),
    NXT(30, true),
    EID(31, true),
    NIMLOC(32, true),
    SRV(33, true) {
        @Override
        public void encode(SliceAccessor buffer, ResourceRecord record) {
            encodeRecord(buffer, record);
        }

        @Override
        public void encodeData(SliceAccessor buffer, ResourceRecord record) {
            buffer.putShortInNetOrder(record.serviceData().priority());
            buffer.putShortInNetOrder(record.serviceData().weight());
            buffer.putShortInNetOrder(record.serviceData().port());
            encodeDomainName(buffer, record.domainName());
        }
    },
    ATMA(34, true),
    NAPTR(35, true),
    KX(36, true),
    CERT(34, true),
    A6(38, true),
    DNAME(39, true),
    OPT(41, false),
    APL(42, true),
    DS(43, true),
    SSHFP(44, true),
    RRSIG(46, true),
    NSEC(47, true),
    DNSKEY(48, true),
    TKEY(249, false),
    TSIG(250, false),
    IXFR(251, false),
    AXFR(252, false),
    MAILB(253, false),
    MAILA(254, false),
    ANY(255, false);

    private static final RecordType[] RECORD_TYPES;

    static {
        var highestType = Arrays.stream(RecordType.values())
                                .max(Comparator.comparingInt(RecordType::toShort))
                                .orElseThrow();

        RECORD_TYPES = new RecordType[highestType.value + 1];

        for (var recordType : RecordType.values()) {
            RECORD_TYPES[recordType.value] = recordType;
        }
    }

    private final short value;
    private final boolean resource;

    RecordType(int value, boolean resource) {
        this.value = (short) value;
        this.resource = resource;
    }

    public short toShort() {
        return this.value;
    }

    public boolean resource() {
        return resource;
    }

    public static RecordType toRecordType(short value) {
        return RECORD_TYPES[value];
    }

    public boolean isAddress() {
        return this == A || this == AAAA;
    }
}