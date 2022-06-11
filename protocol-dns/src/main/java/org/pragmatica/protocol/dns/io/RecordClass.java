/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */

package org.pragmatica.protocol.dns.io;

public enum RecordClass {
    IN(1),
    CS(2),
    CH(3),
    HS(4),
    NONE(254),
    ANY(255);

    private final short value;

    RecordClass(int value) {
        this.value = (short) value;
    }

    public short toShort() {
        return this.value;
    }

    public static RecordClass toRecordClass(short value) {
        return switch (value) {
            case 1 -> IN;
            case 2 -> CS;
            case 3 -> CH;
            case 4 -> HS;
            case 254 -> NONE;
            case 255 -> ANY;
            default -> throw new IllegalArgumentException("Attempt to convert " + value + " to RecordClass");
        };
    }
}
