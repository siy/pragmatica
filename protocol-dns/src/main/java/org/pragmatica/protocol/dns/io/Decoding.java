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

package org.pragmatica.protocol.dns.io;

import org.pragmatica.io.async.util.SliceAccessor;
import org.pragmatica.lang.Functions;
import org.pragmatica.lang.Functions.FN1;
import org.pragmatica.protocol.dns.DnsAttributes;
import org.pragmatica.protocol.dns.ResourceRecord;
import org.pragmatica.protocol.dns.ResourceRecordBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import static org.pragmatica.protocol.dns.io.RecordClass.toRecordClass;
import static org.pragmatica.protocol.dns.io.RecordType.toRecordType;

public final class Decoding {
    private Decoding() {}

    public static String decodeDomainName(SliceAccessor sliceAccessor) {
        return recurseDomainName(sliceAccessor, new StringBuilder()).toString();
    }

    private static StringBuilder recurseDomainName(SliceAccessor sliceAccessor, StringBuilder domainName) {
        int length = sliceAccessor.getUnsignedByte();

        if (isOffset(length)) {
            int position = sliceAccessor.getUnsignedByte();
            int offset = length & ~(0xc0) << 8;
            int originalPosition = sliceAccessor.position();

            sliceAccessor.position(position + offset);

            recurseDomainName(sliceAccessor, domainName);

            sliceAccessor.position(originalPosition);
        } else if (isLabel(length)) {
            getLabel(sliceAccessor, domainName, length);
            recurseDomainName(sliceAccessor, domainName);
        }

        return domainName;
    }

    private static boolean isOffset(int length) {
        return ((length & 0xc0) == 0xc0);
    }

    private static boolean isLabel(int length) {
        return (length != 0 && (length & 0xc0) == 0);
    }

    private static void getLabel(SliceAccessor sliceAccessor, StringBuilder domainName, int labelLength) {
        domainName.append(new ByteArrayCharSequence(sliceAccessor.getBytes(labelLength)));

//        for (int jj = 0; jj < labelLength; jj++) {
//            domainName.append((char) sliceAccessor.getByte());
//        }

        if (sliceAccessor.peekByte() != 0) {
            domainName.append(".");
        }
    }

    public static OpCode decodeOpCode(byte header) {
        return OpCode.fromByte((byte) ((header & 0x78) >>> 3));
    }

    public static boolean decodeAuthoritativeAnswer(byte header) {
        return ((header & 0x04) >>> 2) == 1;
    }

    public static boolean decodeTruncated(byte header) {
        return ((header & 0x02) >>> 1) == 1;
    }

    public static boolean decodeRecursionDesired(byte header) {
        return (header & 0x01) == 1;
    }

    public static boolean decodeRecursionAvailable(byte header) {
        return ((header & 0x80) >>> 7) == 1;
    }

    public static ResponseCode decodeResponseCode(byte header) {
        return ResponseCode.fromByte((byte) (header & 0x0F));
    }

    public static List<ResourceRecord> decodeRecords(SliceAccessor sliceAccessor, short recordCount) {
        return IntStream.range(0, recordCount)
                        .mapToObj(__ -> decodeRecord(sliceAccessor)).toList();
    }

    private static ResourceRecord decodeRecord(SliceAccessor sliceAccessor) {
        return ResourceRecordBuilder.create(decodeDomainName(sliceAccessor))
                                    .recordType(toRecordType(sliceAccessor.getShortInNetOrder()))
                                    .recordClass(toRecordClass(sliceAccessor.getShortInNetOrder()))
                                    .ttl(sliceAccessor.getIntInNetOrder())
                                    .attributes(readAttributes(sliceAccessor))
                                    .build();
    }

    private static FN1<DnsAttributes, RecordType> readAttributes(SliceAccessor sliceAccessor) {
        return recordType -> recordType.decode(sliceAccessor,
                                               AttributeBuilder.create(),
                                               sliceAccessor.getShortInNetOrder())
                                       .build();
    }

    public static List<QuestionRecord> decodeQuestions(SliceAccessor sliceAccessor, short questionCount) {
        return IntStream.range(0, questionCount)
                        .mapToObj(__ -> decodeQuestionRecord(sliceAccessor))
                        .toList();
    }

    private static QuestionRecord decodeQuestionRecord(SliceAccessor sliceAccessor) {
        return new QuestionRecord(decodeDomainName(sliceAccessor), toRecordType(sliceAccessor.getShortInNetOrder()),
                                  toRecordClass(sliceAccessor.getShortInNetOrder()));
    }

    public static MessageType decodeMessageType(byte header) {
        return MessageType.fromByte((byte) ((header & 0x80) >>> 7));
    }

    private static class ByteArrayCharSequence implements CharSequence {
        private final byte[] bytes;

        public ByteArrayCharSequence(byte[] bytes) {
            this.bytes = bytes;
        }

        @Override
        public int length() {
            return bytes.length;
        }

        @Override
        public char charAt(int index) {
            return (char) bytes[index];
        }

        @Override
        public CharSequence subSequence(int start, int end) {
            throw new UnsupportedOperationException();
        }
    }
}
