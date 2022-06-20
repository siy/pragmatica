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

import org.pragmatica.protocol.http.uri.UserInfo;

import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.StringTokenizer;

/**
 * Percent-encoding according to the URI and URL standards.
 */
public final class Encoder {

    private static final boolean IS_PATH = true;

    private static final boolean IS_NOT_PATH = false;

    private static final boolean IS_FRAGMENT = true;

    private static final boolean IS_NOT_FRAGMENT = false;

    private static final boolean IS_USERINFO = true;

    private static final boolean IS_NOT_USERINFO = false;

    private Encoder() {}

    public static String encodeUserInfo(UserInfo input) {
        return urlEncode(input.forIRI(), IS_NOT_PATH, IS_NOT_FRAGMENT, IS_USERINFO);
    }

    public static String encodePath(String input) {
        var sb = new StringBuilder();
        var st = new StringTokenizer(input, "/", true);

        while (st.hasMoreElements()) {
            var element = st.nextToken();

            if ("/".equals(element)) {
                sb.append(element);
            } else if (!element.isEmpty()) {
                sb.append(urlEncode(element, IS_PATH, IS_NOT_FRAGMENT, IS_NOT_USERINFO));
            }
        }
        return sb.toString();
    }

    public static String encodeQueryElement(String input) {
        return urlEncode(input, IS_NOT_PATH, IS_NOT_FRAGMENT, IS_NOT_USERINFO);
    }

    public static String encodeFragment(String input) {
        return urlEncode(input, IS_NOT_PATH, IS_FRAGMENT, IS_NOT_USERINFO);
    }

    public static String urlEncode(String input, boolean isPath, boolean isFragment, boolean isUserInfo) {
        if (input.isEmpty()) {
            return "";
        }

        var sb = new StringBuilder();
        var inputChars = input.toCharArray();

        for (int i = 0; i < Character.codePointCount(inputChars, 0, inputChars.length); i++) {
            int codePoint = Character.codePointAt(inputChars, i);

            var cb = encodeCodePoint(codePoint, sb, isPath, isFragment, isUserInfo);

            if (cb == null) {
                continue;
            }

            encodeBytes(sb, cb);
        }
        return sb.toString();
    }

    private static void encodeBytes(StringBuilder sb, CharBuffer cb) {
        cb.rewind();

        var bb = StandardCharsets.UTF_8.encode(cb);

        for (int j = 0; j < bb.limit(); j++) {
            sb.append('%');
            sb.append(String.format(Locale.US, "%1$02X", bb.get(j)));
        }
    }

    private static CharBuffer encodeCodePoint(int codePoint, StringBuilder sb, boolean isPath, boolean isFragment, boolean isUserInfo) {
        if (Character.isBmpCodePoint(codePoint)) {
            char c = Character.toChars(codePoint)[0];

            if ((isPath && Rfc3986Util.isPChar(c))
                || isFragment && Rfc3986Util.isFragmentSafe(c)
                || isUserInfo && c == ':'
                || Rfc3986Util.isUnreserved(c)) {
                sb.append(c);
                return null;
            } else {
                return CharBuffer.allocate(1).append(c);
            }
        } else {
            return CharBuffer.allocate(2)
                             .append(Character.highSurrogate(codePoint))
                             .append(Character.lowSurrogate(codePoint));
        }
    }

}
