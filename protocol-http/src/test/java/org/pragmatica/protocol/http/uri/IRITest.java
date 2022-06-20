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

package org.pragmatica.protocol.http.uri;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.pragmatica.lang.Option.option;

class IRITest {
    @Test
    void emptyIRI() {
        var iri = IRI.fromString("");

        assertEquals("", iri.toString());
    }

    @Test
    void basicIRI() {
        assertEquals("https://www.google.com/?q=test", IRI.fromString("http://www.google.com/?q=test").withScheme("https").toString());
        assertEquals("http://www.example.com/a%20b/", IRI.fromString("http://www.example.com/a%20b/").toString());
        assertEquals("http://www.example.com/a+b+/", IRI.fromString("http://www.example.com/a+b%2b/").toString());
        assertEquals("http://example.com/a=&b/", IRI.fromString("http://example.com/a=&b/").toString());
        assertEquals("http://example.com/?some%20key=some%20value", IRI.fromString("http://example.com/?some+key=some%20value").toString());
        assertEquals("http://example.com/?some%20%2B%20key=some%20%3D%3Fvalue",
                     IRI.fromString("http://example.com/?some+%2b%20key=some%20%3d?value").toString());
        assertEquals("http://example.com/#=?%23", IRI.fromString("http://example.com/#=?%23").toString());

        var iri1 = IRI.fromString("https://bob:passwd@example.com/secure");
        assertEquals("https://bob:passwd@example.com/secure", iri1.toString());
        assertEquals(option("bob:passwd"), iri1.userInfo().map(UserInfo::forIRI));
        assertEquals(option("example.com"), iri1.hostName());

        var iri2 = IRI.fromString("https://bobby%20droptables:passwd@example.com/secure");
        assertEquals("https://bobby%20droptables:passwd@example.com/secure", iri2.toString());
        assertEquals(option("bobby droptables:passwd"), iri2.userInfo().map(UserInfo::forIRI));

        assertEquals("mailto:bob@example.com", IRI.fromString("mailto:bob@example.com").toString());
    }
}