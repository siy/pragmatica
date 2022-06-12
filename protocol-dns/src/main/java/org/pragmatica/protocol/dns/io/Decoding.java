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
import org.pragmatica.lang.Functions.FN1;
import org.pragmatica.protocol.dns.DnsAttributes;
import org.pragmatica.protocol.dns.ResourceRecord;
import org.pragmatica.protocol.dns.ResourceRecordBuilder;

import java.util.List;
import java.util.stream.IntStream;

import static org.pragmatica.protocol.dns.io.RecordClass.toRecordClass;
import static org.pragmatica.protocol.dns.io.RecordType.toRecordType;

//TODO: switch to Result<T>, more error checking
public final class Decoding {
    private Decoding() {}

    public static String decodeDomainName(SliceAccessor sliceAccessor) {
        return decodeDomainName(sliceAccessor, new StringBuilder()).toString();
    }

    private static StringBuilder decodeDomainName(SliceAccessor sliceAccessor, StringBuilder domainName) {
        var length = sliceAccessor.getUnsignedByte();

        if (isOffset(length)) {
            int position = sliceAccessor.getUnsignedByte();
            int offset = length & ~(0xc0) << 8;
            int originalPosition = sliceAccessor.position();

            sliceAccessor.position(position + offset);

            decodeDomainName(sliceAccessor, domainName);

            sliceAccessor.position(originalPosition);
        } else if (isLabel(length)) {
            decideLabel(sliceAccessor, domainName, length);
            decodeDomainName(sliceAccessor, domainName);
        }

        return domainName;
    }

    private static boolean isOffset(int length) {
        return ((length & 0xc0) == 0xc0);
    }

    private static boolean isLabel(int length) {
        return (length != 0 && (length & 0xc0) == 0);
    }

    private static void decideLabel(SliceAccessor sliceAccessor, StringBuilder domainName, int labelLength) {
        domainName.append(new ByteArrayCharSequence(sliceAccessor.getBytes(labelLength)));

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
        var builder = ResourceRecordBuilder.create(decodeDomainName(sliceAccessor));

        int pos = sliceAccessor.position();
        var bytes = sliceAccessor.getBytes(4);
        sliceAccessor.position(pos);

        var recordType = toRecordType(sliceAccessor.getShortInNetOrder());
        var recordClass = toRecordClass(sliceAccessor.getShortInNetOrder());
        var ttl = sliceAccessor.getIntInNetOrder();
        var attributes = readAttributes(sliceAccessor);

        return builder.recordType(recordType)
                      .recordClass(recordClass)
                      .ttl(ttl)
                      .attributes(attributes)
                      .build();

    }

    private static FN1<DnsAttributes, RecordType> readAttributes(SliceAccessor sliceAccessor) {
        int length = sliceAccessor.getUnsignedShortInNetOrder();
        int pos = sliceAccessor.position();

        return recordType -> {
            var attributes = recordType.decode(sliceAccessor,
                                               AttributeBuilder.create(),
                                               length)
                                       .build();
            sliceAccessor.position(pos + length);
            return attributes;
        };
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
