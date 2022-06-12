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

package org.pragmatica.protocol.dns;


import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.io.async.util.SliceAccessor;
import org.pragmatica.protocol.dns.io.*;

import java.util.List;

import static org.pragmatica.protocol.dns.io.Decoding.*;
import static org.pragmatica.protocol.dns.io.Encoding.encodeQuestionRecords;
import static org.pragmatica.protocol.dns.io.Encoding.encodeResourceRecords;

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

    public static DnsMessage decode(OffHeapSlice buffer) {
        return decode(SliceAccessor.forSlice(buffer));
    }

    public static DnsMessage decode(SliceAccessor buffer) {
        var builder = DnsMessageBuilder.create();

        builder.transactionId(buffer.getUnsignedShortInNetOrder());

        byte header = buffer.getByte();
        builder.messageType(decodeMessageType(header));
        builder.opCode(decodeOpCode(header));
        builder.authoritativeAnswer(decodeAuthoritativeAnswer(header));
        builder.truncated(decodeTruncated(header));
        builder.recursionDesired(decodeRecursionDesired(header));

        header = buffer.getByte();
        builder.recursionAvailable(decodeRecursionAvailable(header));
        builder.responseCode(decodeResponseCode(header));

        short questionCount = buffer.getShortInNetOrder();
        short answerCount = buffer.getShortInNetOrder();
        short authorityCount = buffer.getShortInNetOrder();
        short additionalCount = buffer.getShortInNetOrder();

        builder.questionRecords(decodeQuestions(buffer, questionCount));
        builder.answerRecords(decodeRecords(buffer, answerCount));
        builder.authorityRecords(decodeRecords(buffer, authorityCount));
        builder.additionalRecords(decodeRecords(buffer, additionalCount));

        return builder.build();
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

        encodeQuestionRecords(sliceAccessor, questionRecords());
        encodeResourceRecords(sliceAccessor, answerRecords());
        encodeResourceRecords(sliceAccessor, authorityRecords());
        encodeResourceRecords(sliceAccessor, additionalRecords());

        sliceAccessor.updateSlice();
    }
}
