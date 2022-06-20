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

import org.pragmatica.lang.Functions;
import org.pragmatica.lang.Option;

import static org.pragmatica.lang.Option.empty;

public record UserInfo(Option<String> userName, Option<String> password) {
    public static final UserInfo EMPTY = new UserInfo(empty(), empty());

    public boolean isEmpty() {
        return userName.isEmpty() && password.isEmpty();
    }

    public String forIRI() {
        if (userName.isEmpty()) {
            return "";
        }

        if (password.isEmpty()) {
            return userName.fold(() -> "", Functions::id);
        }

        return Option.all(userName, password)
                     .id()
                     .fold(() -> "", tuple -> tuple.map((name, pass) -> name + ':' + pass));
    }
}
