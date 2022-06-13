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

package org.pragmatica.dns.io;

import org.pragmatica.io.async.net.InetAddress.Inet4Address;
import org.pragmatica.io.async.util.SliceAccessor;
import org.pragmatica.dns.ResourceRecord;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static java.util.Arrays.stream;

public final class Encoding {
    private Encoding() {}

    public static void encodeDomainName(SliceAccessor sliceAccessor, String domainName) {
        if (!domainName.isBlank()) {
            stream(domainName.split("\\."))
                .forEach(characterString -> encodeCharacterString(sliceAccessor, characterString));
        }

        sliceAccessor.putByte((byte) 0x00);
    }

    public static void encodeRecordType(SliceAccessor sliceAccessor, RecordType recordType) {
        sliceAccessor.putShortInNetOrder(recordType.toShort());
    }

    public static void encodeRecordClass(SliceAccessor sliceAccessor, RecordClass recordClass) {
        sliceAccessor.putShortInNetOrder(recordClass.toShort());
    }

    public static void encodeQuestionRecords(SliceAccessor sliceAccessor, List<QuestionRecord> questions) {
        questions.forEach(question -> encodeQuestionRecord(sliceAccessor, question));
    }

    private static void encodeQuestionRecord(SliceAccessor sliceAccessor, QuestionRecord question) {
        encodeDomainName(sliceAccessor, question.domainName());
        encodeRecordType(sliceAccessor, question.recordType());
        encodeRecordClass(sliceAccessor, question.recordClass());
    }

    public static void encodeResourceRecords(SliceAccessor sliceAccessor, List<ResourceRecord> records) {
        records.forEach(record -> record.recordType().encode(sliceAccessor, record));
    }

    public static void encodeDataSize(SliceAccessor sliceAccessor, int startPosition) {
        var endPosition = sliceAccessor.position();
        var length = (short) (endPosition - startPosition - 2);

        sliceAccessor.position(startPosition);
        sliceAccessor.putShortInNetOrder(length);
        sliceAccessor.position(endPosition);
    }

    public static void encodeCharacterString(SliceAccessor sliceAccessor, String characterString) {
        sliceAccessor
            .putByte((byte) characterString.length())
            .putBytes(characterString.getBytes(StandardCharsets.UTF_8));
    }

    public static void encodeIp4Address(SliceAccessor sliceAccessor, Inet4Address inetAddress) {
        sliceAccessor.putBytes(inetAddress.asBytes());
    }
}
