package org.pragmatica.protocol.http.parser;

import org.pragmatica.lang.Option;

import java.nio.charset.StandardCharsets;

public enum HttpMethod {
    DELETE,
    GET,
    HEAD,
    POST,
    PUT,
    OPTIONS,
    CONNECT,
    TRACE,
    COPY,
    LOCK,
    MERGE,
    MKCOL,
    MOVE,
    NOTIFY,
    PATCH,
    REPORT,
    UNLOCK;

    private final byte[] nameBytes;
    private Option<HttpMethod> option;

    HttpMethod() {
        this.nameBytes = name().getBytes(StandardCharsets.ISO_8859_1);
        this.option = Option.present(this);
    }

    private Option<HttpMethod> option() {
        return option;
    }

    private static final int[] HASH_VALUES = {
        26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26,
        26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26,
        26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26,
        26, 26, 26, 26, 26, 0, 26, 5, 15, 0, 26, 5, 0, 26, 26, 10, 15, 10, 0, 5,
        0, 26, 10, 26, 5, 0, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 0, 26, 5,
        15, 0, 26, 5, 0, 26, 26, 10, 15, 10, 0, 5, 0, 26, 10, 26, 5, 0, 26, 26,
        26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26,
        26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26,
        26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26,
        26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26,
        26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26,
        26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26,
        26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26, 26
    };

    private static int hash(byte[] str, int offset, int len) {
        return len + HASH_VALUES[str[offset] & 0xFF] + HASH_VALUES[str[offset + 1] & 0xFF];
    }

    private static final HttpMethod[] MAP = {
        null, null, null, PUT, HEAD, PATCH, UNLOCK, null,
        GET, POST, null, NOTIFY, OPTIONS, null, COPY, MERGE,
        REPORT, CONNECT, null, MOVE, TRACE, DELETE, null, null,
        LOCK, MKCOL
    };

    public static Option<HttpMethod> lookup(byte[] str, int offset, int len) {
        if (len <= 7 && len >= 3) {
            var key = hash(str, offset, len);

            if (key <= 25) {
                var method = MAP[key];
                var name = method.nameBytes;

                if (((str[offset] & 0xFF) ^ (name[0] & 0xFF) & ~32) == 0 && ParserConstants.compare(str, offset, name, len) == 0 && name.length == len) {
                    return method.option();
                }
            }
        }

        return Option.none();
    }

}
