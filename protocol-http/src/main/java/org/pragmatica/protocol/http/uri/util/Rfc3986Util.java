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
package org.pragmatica.protocol.http.uri.util;

import java.util.Arrays;

final class Rfc3986Util {

    private static final char[] SUB_DELIMITERS = { '!', '$', '&', '\'', '(', ')', '*', '+', ',', ';', '=' };

    static {
        Arrays.sort(SUB_DELIMITERS);
    }

    static boolean isFragmentSafe(final char c) {
        return isPChar(c)
                || c == '/'
                || c == '?';
    }

    static boolean isPChar(final char c) {
        // Excludes % used in %XX chars
        return isUnreserved(c)
                || isSubDelimeter(c)
                || c == ':'
                || c == '@';
    }

    static boolean isUnreserved(final char c) {
        return ('a' <= c && c <= 'z') ||
                ('A' <= c && c <= 'Z') ||
                ('0' <= c && c <= '9') ||
                (c == '-' || c == '.' || c == '_' || c == '~');
    }

    private static boolean isSubDelimeter(final char c) {
        return Arrays.binarySearch(SUB_DELIMITERS, c) >= 0;
    }

}
