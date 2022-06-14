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


import org.pragmatica.dns.ResourceRecord;
import org.pragmatica.io.async.util.SliceAccessor;

public interface RecordEncoder {
    default void encode(SliceAccessor buffer, ResourceRecord record) {
    }

    default void encodeRecord(SliceAccessor buffer, ResourceRecord record) {
        Encoding.encodeDomainName(buffer, record.domainName());
        Encoding.encodeRecordType(buffer, record.recordType());
        Encoding.encodeRecordClass(buffer, record.recordClass());

        buffer.putInt(record.ttl());

        int startPosition = buffer.position();
        buffer.position(startPosition + 2);

        encodeData(buffer, record);

        Encoding.encodeDataSize(buffer, startPosition);
    }

    default void encodeData(SliceAccessor buffer, ResourceRecord record) {
    }
}
