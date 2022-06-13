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

import org.pragmatica.dns.io.RecordClass;
import org.pragmatica.dns.io.RecordType;

public final class ResourceRecordBuilder {
    private ResourceRecordBuilder() {}

    public interface ResourceRecordBuilderStage1 {
        ResourceRecordBuilderStage2 recordType(RecordType recordType);
    }

    public interface ResourceRecordBuilderStage2 {
        ResourceRecordBuilderStage3 recordClass(RecordClass recordClass);
    }

    public interface ResourceRecordBuilderStage3 {
        ResourceRecordBuilderStage4 ttl(int ttl);
    }

    public interface ResourceRecordBuilderStage4 {
        ResourceRecordBuilderStage5 attributes(DnsAttributes attributes);
    }

    public interface ResourceRecordBuilderStage5 {
        ResourceRecord build();
    }

    public static ResourceRecordBuilderStage1 create(String dnsName) {
        return recordType -> recordClass -> ttl -> attributes ->
            () -> new ResourceRecord(dnsName,
                                     recordType,
                                     recordClass,
                                     ttl,
                                     attributes);
    }
}
