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

package org.pragmatica.protocol.http.parser.header;

import java.nio.charset.StandardCharsets;

import static org.pragmatica.protocol.http.parser.util.ParserHelper.isSame;

public enum StandardHttpHeaderNames implements HeaderName {
    HOST("Host", false),
    CACHE_CONTROL("Cache-Control", true),
    CONNECTION("Connection", false),
    ACCEPT("Accept", true),
    ACCEPT_LANGUAGE("Accept-Language", true),
    ACCEPT_ENCODING("Accept-Encoding", true),
    USER_AGENT("User-Agent", false),
    REFERER("Referer", false),
    X_FORWARDED_FOR("X-Forwarded-For", true),
    ORIGIN("Origin", false),
    UPGRADE_INSECURE_REQUESTS("Upgrade-Insecure-Requests", false),
    PRAGMA("Pragma", true),
    COOKIE("Cookie", false),
    DNT("DNT", false),
    SEC_GPC("Sec-GPC", false),
    FROM("From", false),
    IF_MODIFIED_SINCE("If-Modified-Since", false),
    X_REQUESTED_WITH("X-Requested-With", false),
    X_FORWARDED_HOST("X-Forwarded-Host", false),
    X_FORWARDED_PROTO("X-Forwarded-Proto", false),
    X_CSRF_TOKEN("X-CSRF-Token", false),
    SAVE_DATA("Save-Data", false),
    RANGE("Range", false),
    CONTENT_LENGTH("Content-Length", false),
    CONTENT_TYPE("Content-Type", false),
    VARY("Vary", true),
    DATE("Date", false),
    SERVER("Server", false),
    EXPIRES("Expires", false),
    CONTENT_ENCODING("Content-Encoding", true),
    LAST_MODIFIED("Last-Modified", false),
    ETAG("ETag", false),
    ALLOW("Allow", true),
    CONTENT_RANGE("Content-Range", false),
    ACCEPT_CHARSET("Accept-Charset", true),
    ACCESS_CONTROL_ALLOW_CREDENTIALS("Access-Control-Allow-Credentials", false),
    ACCESS_CONTROL_ALLOW_HEADERS("Access-Control-Allow-Headers", true),
    ACCESS_CONTROL_ALLOW_METHODS("Access-Control-Allow-Methods", true),
    ACCESS_CONTROL_ALLOW_ORIGIN("Access-Control-Allow-Origin", false),
    ACCESS_CONTROL_MAX_AGE("Access-Control-MaxAge", false),
    ACCESS_CONTROL_METHOD("Access-Control-Method", false),
    ACCESS_CONTROL_REQUEST_HEADERS("Access-Control-RequestHeaders", true),
    ACCESS_CONTROL_REQUEST_METHOD("Access-Control-Request-Method", false),
    ACCESS_CONTROL_REQUEST_METHODS("Access-Control-Request-Methods", true),
    AGE("Age", false),
    AUTHORIZATION("Authorization", false),
    CONTENT_BASE("Content-Base", false),
    CONTENT_DESCRIPTION("Content-Description", false),
    CONTENT_DISPOSITION("Content-Disposition", false),
    CONTENT_LANGUAGE("Content-Language", true),
    CONTENT_LOCATION("Content-Location", false),
    CONTENT_MD5("Content-MD5", false),
    EXPECT("Expect", true),
    IF_MATCH("If-Match", true),
    IF_NONE_MATCH("If-None-Match", true),
    IF_RANGE("If-Range", false),
    IF_UNMODIFIED_SINCE("If-Unmodified-Since", false),
    KEEP_ALIVE("Keep-Alive", false),
    LINK("Link", false),
    LOCATION("Location", false),
    MAX_FORWARDS("Max-Forwards", false),
    PROXY_AUTHENTICATE("Proxy-Authenticate", true),
    PROXY_AUTHORIZATION("Proxy-Authorization", false),
    PROXY_CONNECTION("Proxy-Connection", false),
    PUBLIC("Public", true),
    RETRY_AFTER("Retry-After", false),
    TE("TE", true),
    TRAILER("Trailer", true),
    TRANSFER_ENCODING("Transfer-Encoding", true),
    UPGRADE("Upgrade", true),
    WARNING("Warning", true),
    WWW_AUTHENTICATE("WWW-Authenticate", true),
    VIA("Via", true),
    STRICT_TRANSPORT_SECURITY("Strict-Transport-Security", false),
    X_FRAME_OPTIONS("X-Frame-Options", false),
    X_CONTENT_TYPE_OPTIONS("X-Content-Type-Options", false),
    ALT_SVC("Alt-Svc", false),
    REFERRER_POLICY("Referrer-Policy", false),
    X_XSS_PROTECTION("X-XSS-Protection", false),
    ACCEPT_RANGES("Accept-Ranges", false),
    SET_COOKIE("Set-Cookie", false),
    SEC_CH_UA("Sec-CH-UA", false),
    SEC_CH_UA_MOBILE("Sec-CH-UA-Mobile", false),
    SEC_FETCH_SITE("Sec-Fetch-Site", false),
    SEC_FETCH_MODE("Sec-Fetch-Mode", false),
    SEC_FETCH_USER("Sec-Fetch-User", false),
    SEC_FETCH_DEST("Sec-Fetch-Dest", false);

    private final String headerName;
    private final byte[] nameBytes;
    private final boolean repeatable;

    StandardHttpHeaderNames(String headerName, boolean repeatable) {
        this.headerName = headerName;
        this.nameBytes = headerName.getBytes(StandardCharsets.ISO_8859_1);
        this.repeatable = repeatable;
    }

    @Override
    public String canonicalName() {
        return headerName;
    }

    @Override
    public boolean repeatable() {
        return repeatable;
    }

    private static final int[] HASH_VALUES = {
        199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199,
        199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199,
        199, 199, 199, 199, 199, 55, 199, 199, 199, 199, 199, 199, 199, 20, 199, 199, 199, 199, 199, 199,
        199, 199, 199, 199, 199, 15, 199, 30, 35, 10, 25, 15, 0, 70, 199, 55, 25, 40, 0, 45,
        15, 20, 50, 0, 0, 5, 199, 0, 199, 20, 199, 199, 199, 199, 199, 199, 199, 15, 199, 30,
        35, 10, 25, 15, 0, 70, 199, 55, 25, 40, 0, 45, 15, 20, 50, 0, 0, 5, 199, 0,
        199, 20, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199,
        199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199,
        199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199,
        199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199,
        199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199,
        199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199,
        199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199, 199
    };

    private static int hash(byte[] str, int offset, int len) {
        int hashValue = len;

        switch (hashValue)
        {
            default:
                hashValue += HASH_VALUES[str[offset + 21] & 0xFF];
                /*FALLTHROUGH*/
            case 21: case 20: case 19: case 18: case 17: case 16:
            case 15: case 14: case 13: case 12: case 11:
                hashValue += HASH_VALUES[str[offset + 10] & 0xFF];
                /*FALLTHROUGH*/
            case 10:
                hashValue += HASH_VALUES[str[offset + 9] & 0xFF];
                /*FALLTHROUGH*/
            case 9: case 8: case 7: case 6: case 5:
                hashValue += HASH_VALUES[str[offset + 4] & 0xFF];
                /*FALLTHROUGH*/
            case 4: case 3: case 2:
                break;
        }
        
        return hashValue + HASH_VALUES[str[offset + len - 1] & 0xFF];
    }

    private static final StandardHttpHeaderNames[] MAP = {
        null, null, null, DNT, HOST, ALLOW, null, null, LOCATION, null, null, null, TE, AGE, DATE, null, null, null,
        VIA, ETAG, CONNECTION, ACCEPT, null, IF_MATCH, VARY, RANGE, X_XSS_PROTECTION, null,
        ACCESS_CONTROL_ALLOW_HEADERS, ACCESS_CONTROL_REQUEST_HEADERS, ACCESS_CONTROL_REQUEST_METHODS,
        ACCESS_CONTROL_MAX_AGE, UPGRADE, IF_RANGE, CONTENT_LENGTH, null, EXPECT, ALT_SVC, null, CONTENT_DESCRIPTION,
        null, WWW_AUTHENTICATE, TRANSFER_ENCODING, ACCEPT_RANGES, FROM, X_FRAME_OPTIONS, PROXY_CONNECTION, CONTENT_BASE,
        CONTENT_RANGE, null, UPGRADE_INSECURE_REQUESTS, CONTENT_LANGUAGE, SEC_GPC, null, SEC_CH_UA, ACCEPT_LANGUAGE,
        ACCESS_CONTROL_METHOD, EXPIRES, PROXY_AUTHENTICATE, LINK, SET_COOKIE, PRAGMA, ACCESS_CONTROL_ALLOW_CREDENTIALS,
        null, ACCESS_CONTROL_REQUEST_METHOD, USER_AGENT, SERVER, CONTENT_TYPE, ACCESS_CONTROL_ALLOW_METHODS, null, null,
        CONTENT_ENCODING, ACCESS_CONTROL_ALLOW_ORIGIN, IF_NONE_MATCH, null, null, ORIGIN, X_CONTENT_TYPE_OPTIONS, null,
        SAVE_DATA, null, X_REQUESTED_WITH, TRAILER, null, PROXY_AUTHORIZATION, KEEP_ALIVE, COOKIE, null, null, null,
        null, null, WARNING, null, ACCEPT_CHARSET, null, CONTENT_MD5, null, CACHE_CONTROL, CONTENT_DISPOSITION, null,
        CONTENT_LOCATION, null, null, SEC_FETCH_SITE, null, PUBLIC, REFERER, null, null, null, X_FORWARDED_HOST, null,
        null, null, null, null, IF_MODIFIED_SINCE, null, null, ACCEPT_ENCODING, null, MAX_FORWARDS, null,
        IF_UNMODIFIED_SINCE, null, null, X_CSRF_TOKEN, AUTHORIZATION, SEC_FETCH_DEST, null, null, null, null, null,
        null, null, null, null, null, STRICT_TRANSPORT_SECURITY, RETRY_AFTER, null, null, SEC_FETCH_MODE,
        REFERRER_POLICY, null, null, null, SEC_FETCH_USER, null, SEC_CH_UA_MOBILE, null, null, null, null, null,
        X_FORWARDED_PROTO, null, null, X_FORWARDED_FOR, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null,
        null, null, null, null, null, null, null, null, null, LAST_MODIFIED,
    };
    
    public static HeaderName lookup(byte[] str, int offset, int len) {
        if (len <= 32 && len >= 2)
        {
            var key = hash(str, offset, len);

            if (key <= 198)
            {
                var header = MAP[key];

                if (header == null) {
                    return makeCustomHeaderName(str, len);
                }

                var name = header.nameBytes;
                var sameFirstCharIgnoreCase = ((str[offset] & 0xFF) ^ (name[0] & 0xFF) & ~32) == 0;

                if (name.length == len && sameFirstCharIgnoreCase && isSame(str, offset, name, len)) {
                    return header;
                }
            }
        }

        return makeCustomHeaderName(str, len);
    }

    private static HeaderName makeCustomHeaderName(byte[] str, int len) {
        return HeaderName.custom(new String(str, 0, len, StandardCharsets.ISO_8859_1));
    }
}
