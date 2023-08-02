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

public record QuestionRecord(String domainName, RecordType recordType, RecordClass recordClass) {
    public static QuestionRecord addressV4ByName(String domainName) {
        return new QuestionRecord(domainName, RecordType.A, RecordClass.IN);
    }

    public static QuestionRecord addressV6ByName(String domainName) {
        return new QuestionRecord(domainName, RecordType.AAAA, RecordClass.IN);
    }
}
