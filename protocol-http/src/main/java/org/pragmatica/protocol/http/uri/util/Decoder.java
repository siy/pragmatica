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

import org.pragmatica.io.async.net.InetPort;
import org.pragmatica.lang.Causes;
import org.pragmatica.lang.Option;
import org.pragmatica.lang.Result;
import org.pragmatica.protocol.http.uri.UserInfo;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.StringTokenizer;

import static org.pragmatica.lang.Option.empty;
import static org.pragmatica.lang.Option.option;

/**
 * Percent-decoding according to the URI and URL standards.
 */
public final class Decoder {
    public static final boolean DECODE_PLUS_AS_SPACE = true;
    public static final boolean DO_NOT_DECODE_PLUS_AS_SPACE = false;

    private Decoder() {}

    public static UserInfo decodeUserInfo(String userInfo) {
        var text = urlDecode(userInfo, DECODE_PLUS_AS_SPACE);

        if (text.isBlank()) {
            return UserInfo.EMPTY;
        }

        if (!text.contains(":")) {
            return new UserInfo(option(text), empty());
        }

        var separate = text.split(":");

        return new UserInfo(option(separate[0]), option(separate[1]));
    }

    public static String decodeFragment(String fragment) {
        return urlDecode(fragment, DO_NOT_DECODE_PLUS_AS_SPACE);
    }

    public static QueryParameters parseQueryString(String query) {
        var ret = QueryParameters.parameters();
        if (query == null || query.isEmpty()) {
            return ret;
        }
        for (String part : query.split("&")) {
            String[] kvp = part.split("=", 2);
            String key, value;
            key = urlDecode(kvp[0], DECODE_PLUS_AS_SPACE);
            if (kvp.length == 2) {
                value = urlDecode(kvp[1], DECODE_PLUS_AS_SPACE);
            } else {
                value = null;
            }
            ret.add(key, value);
        }
        return ret;
    }

    public static byte[] nextDecodeableSequence(String input, int position) {
        int len = input.length();
        byte[] data = new byte[len];
        int j = 0;
        for (int i = position; i < len; i++) {
            char c0 = input.charAt(i);
            if (c0 != '%' || (len < i + 3)) {
                return Arrays.copyOfRange(data, 0, j);
            } else {
                data[j++] = (byte) Integer.parseInt(input.substring(i + 1, i + 3), 16);
                i += 2;
            }
        }
        return Arrays.copyOfRange(data, 0, j);
    }

    public static String decodePath(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        boolean RETURN_DELIMETERS = true;
        StringTokenizer st = new StringTokenizer(input, "/", RETURN_DELIMETERS);

        while (st.hasMoreElements()) {
            String element = st.nextToken();
            if ("/".equals(element)) {
                sb.append(element);
            } else if (!element.isEmpty()) {
                sb.append(urlDecode(element, DO_NOT_DECODE_PLUS_AS_SPACE));
            }
        }
        return sb.toString();
    }

    public static String urlDecode(String input, boolean decodePlusAsSpace) {
        if (input.isEmpty()) {
            return input;
        }

        StringBuilder sb = new StringBuilder();
        int len = input.length();
        for (int i = 0; i < len; i++) {
            char c0 = input.charAt(i);
            if (c0 == '+' && decodePlusAsSpace) {
                sb.append(' ');
            } else if (c0 != '%') {
                sb.append(c0);
            } else if (len < i + 3) {
                // the string will end before we will be able to read a sequence
                int endIndex = Math.min(input.length(), i + 2);
                sb.append(input, i, endIndex);
                i += 3;
            } else {
                byte[] bytes = nextDecodeableSequence(input, i);
                sb.append(StandardCharsets.UTF_8.decode(ByteBuffer.wrap(bytes)));
                i += bytes.length * 3 - 1;
            }
        }
        return sb.toString();
    }

    public static Option<InetPort> parsePort(String input) {
        return Result.lift(Causes.IRRELEVANT, () -> Integer.parseInt(input)).map(InetPort::inetPort).toOption();
    }
}
