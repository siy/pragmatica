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

import org.pragmatica.io.async.net.InetAddress;
import org.pragmatica.io.async.net.InetAddress.Inet4Address;
import org.pragmatica.lang.Result;
import org.pragmatica.protocol.dns.DnsAttributes;
import org.pragmatica.protocol.dns.DomainData;
import org.pragmatica.protocol.dns.ServiceData;
import org.pragmatica.protocol.dns.SoaData;

public class AttributeBuilder {
    private InetAddress ip = Inet4Address.INADDR_ANY;
    private String domainName = "";
    private short mxReference = (short) 0;

    private AttributeBuilder() {}

    public static AttributeBuilder create() {
        return new AttributeBuilder();
    }

    public AttributeBuilder ip(Result<? extends InetAddress> inet4Address) {
        inet4Address.onSuccess(inetAddress -> ip = inetAddress);
        return this;
    }
    public AttributeBuilder domainName(String domainName) {
        this.domainName = domainName;
        return this;
    }

    public AttributeBuilder mxReference(short mxReference) {
        this.mxReference = mxReference;
        return this;
    }

    public DnsAttributes build() {
        return new DnsAttributes(soaData(), serviceData(), domainData());
    }

    public SoaData soaData() {
        return SoaData.empty();
    }

    private ServiceData serviceData() {
        return ServiceData.empty();
    }

    private DomainData domainData() {
        return new DomainData(domainName, ip, mxReference, "");
    }
}