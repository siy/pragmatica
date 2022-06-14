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


import org.pragmatica.dns.codec.MessageType;
import org.pragmatica.dns.codec.OpCode;
import org.pragmatica.dns.codec.QuestionRecord;
import org.pragmatica.dns.codec.ResponseCode;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

//TODO: make a fluent builder
public final class DnsMessageBuilder {
    private static final AtomicInteger txId = new AtomicInteger(0);


    private int transactionId;
    private MessageType messageType = MessageType.QUERY;
    private OpCode opCode = OpCode.QUERY;
    private boolean authoritativeAnswer;
    private boolean truncated;
    private boolean recursionDesired = true;
    private boolean recursionAvailable;
    private boolean reserved;
    private boolean acceptNonAuthenticatedData;
    private ResponseCode responseCode = ResponseCode.NO_ERROR;
    private final List<QuestionRecord> questionRecords = new ArrayList<>();
    private final List<ResourceRecord> answerRecords = new ArrayList<>();
    private final List<ResourceRecord> authorityRecords = new ArrayList<>();
    private final List<ResourceRecord> additionalRecords = new ArrayList<>();

    private DnsMessageBuilder() {}

    public static DnsMessageBuilder create() {
        return new DnsMessageBuilder().transactionId(txId.incrementAndGet());
    }

    public DnsMessage build() {
        return new DnsMessage(transactionId, messageType, opCode, authoritativeAnswer, truncated, recursionDesired,
                              recursionAvailable, reserved, acceptNonAuthenticatedData, responseCode, questionRecords, answerRecords,
                              authorityRecords, additionalRecords);
    }

    public DnsMessageBuilder transactionId(int transactionId) {
        this.transactionId = transactionId;
        return this;
    }

    public DnsMessageBuilder messageType(MessageType messageType) {
        this.messageType = messageType;
        return this;
    }

    public DnsMessageBuilder opCode(OpCode opCode) {
        this.opCode = opCode;
        return this;
    }

    public DnsMessageBuilder authoritativeAnswer(boolean authoritativeAnswer) {
        this.authoritativeAnswer = authoritativeAnswer;
        return this;
    }

    public DnsMessageBuilder truncated(boolean truncated) {
        this.truncated = truncated;
        return this;
    }

    public DnsMessageBuilder recursionDesired(boolean recursionDesired) {
        this.recursionDesired = recursionDesired;
        return this;
    }

    public DnsMessageBuilder recursionAvailable(boolean recursionAvailable) {
        this.recursionAvailable = recursionAvailable;
        return this;
    }

    public DnsMessageBuilder reserved(boolean reserved) {
        this.reserved = reserved;
        return this;
    }

    public DnsMessageBuilder acceptNonAuthenticatedData(boolean acceptNonAuthenticatedData) {
        this.acceptNonAuthenticatedData = acceptNonAuthenticatedData;
        return this;
    }

    public DnsMessageBuilder responseCode(ResponseCode responseCode) {
        this.responseCode = responseCode;
        return this;
    }

    public DnsMessageBuilder questionRecord(QuestionRecord questionRecord) {
        this.questionRecords.add(questionRecord);
        return this;
    }

    public DnsMessageBuilder questionRecords(List<QuestionRecord> questionRecords) {
        this.questionRecords.addAll(questionRecords);
        return this;
    }

    public DnsMessageBuilder answerRecords(List<ResourceRecord> answerRecords) {
        this.answerRecords.addAll(answerRecords);
        return this;
    }

    public DnsMessageBuilder authorityRecords(List<ResourceRecord> authorityRecords) {
        this.authorityRecords.addAll(authorityRecords);
        return this;
    }

    public DnsMessageBuilder additionalRecords(List<ResourceRecord> additionalRecords) {
        this.additionalRecords.addAll(additionalRecords);
        return this;
    }
}
