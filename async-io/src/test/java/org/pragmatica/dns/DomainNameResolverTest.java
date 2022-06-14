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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Promise;

import java.util.List;

class DomainNameResolverTest {
    @Test
    void trySomeExistentDomains() {
        var resolver = DomainNameResolver.resolver();

        var ibm = resolver.forName("www.ibm.com");
        var google = resolver.forName("www.google.com");
        var github = resolver.forName("www.github.com");
        var twitter = resolver.forName("www.twitter.com");

        Promise.all(ibm, google, github, twitter)
               .map(List::of)
               .onSuccess(list -> list.forEach(System.out::println))
               .join()
               .onFailureDo(Assertions::fail);
    }
}