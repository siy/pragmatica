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

package org.pragmatica.dns;


import org.pragmatica.dns.io.*;
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.io.async.util.SliceAccessor;
import org.pragmatica.lang.Result;

import java.util.List;

public record DnsMessage(
    int transactionId,
    MessageType messageType,
    OpCode opCode,
    boolean authoritativeAnswer,
    boolean truncated,
    boolean recursionDesired,
    boolean recursionAvailable,
    boolean reserved,
    boolean acceptNonAuthenticatedData,
    ResponseCode responseCode,
    List<QuestionRecord> questionRecords,
    List<ResourceRecord> answerRecords,
    List<ResourceRecord> authorityRecords,
    List<ResourceRecord> additionalRecords) {

    // TxID, header, header, question count, answer count, authority count, additional count
    public static final int MIN_MESSAGE_LENGTH = Short.BYTES + Byte.BYTES + Byte.BYTES + Short.BYTES + Short.BYTES + Short.BYTES + Short.BYTES;

    public static Result<DnsMessage> decode(OffHeapSlice offHeapSlice) {
        return decode(SliceAccessor.forSlice(offHeapSlice));
    }

    public static Result<DnsMessage> decode(SliceAccessor sliceAccessor) {
        if (!sliceAccessor.availableBytes(MIN_MESSAGE_LENGTH)) {
            return DnsIoErrors.TOO_SHORT_INPUT.result();
        }

        var builder = DnsMessageBuilder.create().transactionId(sliceAccessor.getUnsignedShortInNetOrder());

        var headerByte1 = sliceAccessor.getByte();
        var headerByte2 = sliceAccessor.getByte();
        var questionCount = sliceAccessor.getShortInNetOrder();
        var answerCount = sliceAccessor.getShortInNetOrder();
        var authorityCount = sliceAccessor.getShortInNetOrder();
        var additionalCount = sliceAccessor.getShortInNetOrder();

        return Decoding.decodeMessageType(headerByte1).map(builder::messageType)
                       .flatMap(__ -> Decoding.decodeOpCode(headerByte1)).map(builder::opCode)
                       .flatMap(__ -> Decoding.decodeAuthoritativeAnswer(headerByte1)).map(builder::authoritativeAnswer)
                       .flatMap(__ -> Decoding.decodeTruncated(headerByte1)).map(builder::truncated)
                       .flatMap(__ -> Decoding.decodeRecursionDesired(headerByte1)).map(builder::recursionDesired)
                       .flatMap(__ -> Decoding.decodeRecursionAvailable(headerByte2)).map(builder::recursionAvailable)
                       .flatMap(__ -> Decoding.decodeResponseCode(headerByte2)).map(builder::responseCode)
                       .flatMap(__ -> Decoding.decodeQuestions(sliceAccessor, questionCount)).map(builder::questionRecords)
                       .flatMap(__ -> Decoding.decodeRecords(sliceAccessor, answerCount)).map(builder::answerRecords)
                       .flatMap(__ -> Decoding.decodeRecords(sliceAccessor, authorityCount)).map(builder::authorityRecords)
                       .flatMap(__ -> Decoding.decodeRecords(sliceAccessor, additionalCount)).map(builder::additionalRecords)
                       .map(DnsMessageBuilder::build);
    }

    private static final byte AUTHORITATIVE_ANSWER = (byte) ((byte) 0x01 << 2);
    private static final byte TRUNCATED = (byte) ((byte) 0x01 << 1);
    private static final byte RECURSION_DESIRED = (byte) 0x01;
    private static final byte RECURSION_AVAILABLE = (byte) ((byte) 0x01 << 7);

    public void encode(OffHeapSlice offHeapSlice) {
        encode(SliceAccessor.forSlice(offHeapSlice));
    }

    public void encode(SliceAccessor sliceAccessor) {
        sliceAccessor.putShortInNetOrder((short) transactionId());

        byte header = (byte) 0x00;
        header |= messageType().asByte();
        header |= opCode().asByte();
        header |= authoritativeAnswer() ? AUTHORITATIVE_ANSWER : (byte) 0;
        header |= truncated() ? TRUNCATED : 0;
        header |= recursionDesired() ? RECURSION_DESIRED : 0;
        sliceAccessor.putByte(header);

        header = (byte) 0x00;
        header |= recursionAvailable() ? RECURSION_AVAILABLE : 0;
        header |= (byte) (this.responseCode().toByte() & 0x0F);
        sliceAccessor.putByte(header);

        sliceAccessor.putShortInNetOrder((short) questionRecords().size());
        sliceAccessor.putShortInNetOrder((short) answerRecords().size());
        sliceAccessor.putShortInNetOrder((short) authorityRecords().size());
        sliceAccessor.putShortInNetOrder((short) additionalRecords().size());

        Encoding.encodeQuestionRecords(sliceAccessor, questionRecords());
        Encoding.encodeResourceRecords(sliceAccessor, answerRecords());
        Encoding.encodeResourceRecords(sliceAccessor, authorityRecords());
        Encoding.encodeResourceRecords(sliceAccessor, additionalRecords());

        sliceAccessor.updateSlice();
    }
}
