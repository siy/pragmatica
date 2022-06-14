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

package org.pragmatica.dns.codec;

import org.pragmatica.dns.DnsAttributes;
import org.pragmatica.dns.ResourceRecord;
import org.pragmatica.io.async.util.SliceAccessor;
import org.pragmatica.lang.Result;
import org.pragmatica.lang.Unit;

import java.util.List;
import java.util.stream.IntStream;

import static org.pragmatica.dns.codec.DnsIoErrors.TOO_SHORT_INPUT;
import static org.pragmatica.dns.codec.RecordType.toRecordType;
import static org.pragmatica.lang.Result.*;
import static org.pragmatica.lang.Unit.unitResult;

public final class Decoding {
    private Decoding() {}

    public static Result<String> decodeDomainName(SliceAccessor sliceAccessor) {
        var builder = new StringBuilder();

        return decodeDomainName(sliceAccessor, builder)
            .map(builder::toString);
    }

    private static Result<Unit> decodeDomainName(SliceAccessor sliceAccessor, StringBuilder domainName) {
        if (!sliceAccessor.availableByte()) {
            return TOO_SHORT_INPUT.result();
        }

        var length = sliceAccessor.getUnsignedByte();

        if (isOffset(length)) {
            if (!sliceAccessor.availableByte()) {
                return TOO_SHORT_INPUT.result();
            }

            int position = sliceAccessor.getUnsignedByte();

            int offset = length & ~(0xc0) << 8;
            int originalPosition = sliceAccessor.position();

            sliceAccessor.position(position + offset);

            return decodeDomainName(sliceAccessor, domainName)
                .onResultDo(() -> sliceAccessor.position(originalPosition));
        } else if (isLabel(length)) {
            return decodeLabel(sliceAccessor, domainName, length)
                .flatMap(() -> decodeDomainName(sliceAccessor, domainName));
        }

        return unitResult();
    }

    private static boolean isOffset(int length) {
        return ((length & 0xc0) == 0xc0);
    }

    private static boolean isLabel(int length) {
        return (length != 0 && (length & 0xc0) == 0);
    }

    private static Result<Unit> decodeLabel(SliceAccessor sliceAccessor, StringBuilder domainName, int labelLength) {
        if (!sliceAccessor.availableBytes(labelLength)) {
            return TOO_SHORT_INPUT.result();
        }

        domainName.append(new ByteArrayCharSequence(sliceAccessor.getBytes(labelLength)));

        if (sliceAccessor.peekByte() != 0) {
            domainName.append(".");
        }

        return unitResult();
    }

    public static Result<OpCode> decodeOpCode(byte header) {
        return OpCode.fromByte((byte) ((header & 0x78) >>> 3));
    }

    public static Result<Boolean> decodeAuthoritativeAnswer(byte header) {
        return success(((header & 0x04) >>> 2) == 1);
    }

    public static Result<Boolean> decodeTruncated(byte header) {
        return success(((header & 0x02) >>> 1) == 1);
    }

    public static Result<Boolean> decodeRecursionDesired(byte header) {
        return success((header & 0x01) == 1);
    }

    public static Result<Boolean> decodeRecursionAvailable(byte header) {
        return success(((header & 0x80) >>> 7) == 1);
    }

    public static Result<ResponseCode> decodeResponseCode(byte header) {
        return ResponseCode.fromByte((byte) (header & 0x0F));
    }

    public static Result<List<ResourceRecord>> decodeRecords(SliceAccessor sliceAccessor, short recordCount) {
        return allOf(IntStream.range(0, recordCount)
                              .mapToObj(__ -> decodeRecord(sliceAccessor)).toList());
    }

    private static Result<ResourceRecord> decodeRecord(SliceAccessor sliceAccessor) {
        var domainName = decodeDomainName(sliceAccessor);

        // Record type + record class + ttl + attributes length
        if (!sliceAccessor.availableBytes(Short.BYTES + Short.BYTES + Integer.BYTES + Short.BYTES)) {
            return TOO_SHORT_INPUT.result();
        }

        var recordType = toRecordType(sliceAccessor.getShortInNetOrder());
        var recordClass = RecordClass.toRecordClass(sliceAccessor.getShortInNetOrder());
        var ttl = success(sliceAccessor.getIntInNetOrder());
        var attributes = readAttributes(sliceAccessor, recordType);

        return all(domainName, recordType, recordClass, ttl, attributes).map(ResourceRecord::new);
    }

    private static Result<DnsAttributes> readAttributes(SliceAccessor sliceAccessor, Result<RecordType> recordType) {
        var length = sliceAccessor.getUnsignedShortInNetOrder();
        var pos = sliceAccessor.position();

        return recordType.flatMap(type -> type.decode(sliceAccessor, AttributeBuilder.create(), length))
                         .map(AttributeBuilder::build)
                         .onResultDo(() -> sliceAccessor.position(pos + length));
    }

    public static Result<List<QuestionRecord>> decodeQuestions(SliceAccessor sliceAccessor, short questionCount) {
        return allOf(IntStream.range(0, questionCount)
                              .mapToObj(__ -> decodeQuestionRecord(sliceAccessor))
                              .toList());
    }

    private static Result<QuestionRecord> decodeQuestionRecord(SliceAccessor sliceAccessor) {
        return all(decodeDomainName(sliceAccessor),
                   toRecordType(sliceAccessor.getShortInNetOrder()),
                   RecordClass.toRecordClass(sliceAccessor.getShortInNetOrder()))
            .map(QuestionRecord::new);
    }

    public static Result<MessageType> decodeMessageType(byte header) {
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
