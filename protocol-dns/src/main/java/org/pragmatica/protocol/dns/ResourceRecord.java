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

import org.pragmatica.protocol.dns.io.RecordClass;
import org.pragmatica.protocol.dns.io.RecordType;
public record ResourceRecord(String domainName, RecordType recordType, RecordClass recordClass, int ttl, DnsAttributes attributes) {
    public ServiceData serviceData() {
        return attributes.serviceData();
    }

    public SoaData soaData() {
        return attributes.soaData();
    }

    public DomainData domainData() {
        return attributes.domainData();
    }
}