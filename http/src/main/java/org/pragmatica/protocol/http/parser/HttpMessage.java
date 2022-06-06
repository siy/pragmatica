/*
 * Based on the https://github.com/jart/cosmopolitan
 *
 * Original copyright notice:
 * Copyright 2020 Justine Alexandra Roberts Tunney
 *
 * Permission to use, copy, modify, and/or distribute this software for
 * any purpose with or without fee is hereby granted, provided that the
 * above copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL
 * WARRANTIES WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE
 * AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL
 * DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR
 * PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER
 * TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
 * PERFORMANCE OF THIS SOFTWARE.
 */
package org.pragmatica.protocol.http.parser;

import org.pragmatica.lang.Option;
import org.pragmatica.lang.Option.Some;
import org.pragmatica.lang.Result;
import org.pragmatica.protocol.http.parser.ParsingState.Continue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.pragmatica.lang.Result.success;
import static org.pragmatica.protocol.http.parser.ParserConstants.*;
import static org.pragmatica.protocol.http.parser.HttpMessage.HttpParserState.*;

public class HttpMessage {
    private int index;
    private int lookup;
    private int status;
    private HttpParserState parserState = START;
    private ParserType type;
    private HttpMethod method;
    private int version;
    private final Slice header = new Slice(0, 0);
    private final Slice uri = new Slice(0, 0);
    private final Slice message = new Slice(0, 0);
    private final List<HttpHeader> headers = new ArrayList<>();
    private final Slice xmethod = new Slice(0, 0);

    private HttpMessage(ParserType type) {
        this.type = type;
    }

    public static HttpMessage forRequest() {
        return new HttpMessage(ParserType.REQUEST);
    }

    public static HttpMessage forResponse() {
        return new HttpMessage(ParserType.RESPONSE);
    }

    public String text() {
        var builder = new StringBuilder();

        builder
            .append("Method: ")
            .append(method)
            .append("\n")
            .append("Version: ")
            .append(version)
            .append("\n")
            .append("Headers: ")
            .append(headers.size())
            .append("\n");


        for (var h : headers) {
            builder.append("  ").append(h.text()).append("\n");
        }

        return builder.toString();
    }

    public Result<ParsingState> parse(byte[] p) {
        int c, i;
        int n = p.length;

        for (; index < n; ++index) {
            c = p[index] & 0xff;

            switch (parserState) {
                case START:
                    if (c == '\r' || c == '\n') {
                        break; /* RFC7230 § 3.5 */
                    }
                    if (kHttpToken[c] == 0) {
                        return BAD_MSG;
                    }
                    parserState = type == ParserType.REQUEST ? METHOD : VERSION;
                    lookup = index;
                    break;
                case METHOD:
                    for (; ; ) {
                        if (c == ' ') {
                            var httpMethod = HttpMethod.lookup(p, lookup, index - lookup);

//                            switch (httpMethod) {
//                                case Some some -> method = some.value();
//                            }

//                            method = httpMethod;
                            xmethod.start = lookup;
                            xmethod.end = index;

                            lookup = index + 1;
                            parserState = URI;
                            break;
                        } else if (kHttpToken[c] == 0) {
                            return BAD_MSG;
                        }
                        if (++index == n) {
                            break;
                        }
                        c = p[index] & 0xff;
                    }
                    break;
                case URI:
                    for (; ; ) {
                        if (c == ' ' || c == '\r' || c == '\n') {
                            if (index == lookup) {
                                return BAD_MSG;
                            }

                            uri.start = lookup;
                            uri.end = index;

                            if (c == ' ') {
                                lookup = index + 1;
                                parserState = VERSION;
                            } else {
                                version = 9;
                                parserState = c == '\r' ? CR : LF1;
                            }
                            break;
                        } else if (c < 0x20 || (0x7F <= c && c < 0xA0)) {
                            return BAD_MSG;
                        }
                        if (++index == n) {break;}
                        c = p[index] & 0xff;
                    }
                    break;
                case VERSION:
                    if (c == ' ' || c == '\r' || c == '\n') {
                        if (index - lookup == 8 &&
                            (READ64BE(p, lookup) & 0xFFFFFFFFFF00FF00L) == 0x485454502F002E00L
                            && isDigit(p[lookup + 5])
                            && isDigit(p[lookup + 7])) {

                            version = (p[lookup + 5] - '0') * 10 + (p[lookup + 7] - '0');
                            if (type == ParserType.REQUEST) {
                                parserState = c == '\r' ? CR : LF1;
                            } else {
                                parserState = STATUS;
                            }
                        } else {
                            return BAD_MSG;
                        }
                    }
                    break;
                case STATUS:
                    for (; ; ) {
                        if (c == ' ' || c == '\r' || c == '\n') {
                            if (status < 100) {
                                return BAD_MSG;
                            }
                            if (c == ' ') {
                                lookup = index + 1;
                                parserState = MESSAGE;
                            } else {
                                parserState = c == '\r' ? CR : LF1;
                            }
                            break;
                        } else if ('0' <= c && c <= '9') {
                            status *= 10;
                            status += c - '0';
                            if (status > 999) {
                                return BAD_MSG;
                            }
                        } else {
                            return BAD_MSG;
                        }
                        if (++index == n) {
                            break;
                        }
                        c = p[index] & 0xff;
                    }
                    break;

                case MESSAGE:
                    for (; ; ) {
                        if (c == '\r' || c == '\n') {
                            message.start = lookup;
                            message.end = index;
                            parserState = c == '\r' ? CR : LF1;
                            break;
                        } else if (c < 0x20 || (0x7F <= c && c < 0xA0)) {
                            return BAD_MSG;
                        }
                        if (++index == n) {break;}
                        c = p[index] & 0xff;
                    }
                    break;

                case CR:
                    if (c != '\n') {
                        return BAD_MSG;
                    }
                    parserState = LF1;
                    break;

                case LF1:
                    if (c == '\r') {
                        parserState = LF2;
                        break;
                    } else if (c == '\n') {
                        return success(new Continue(++index));
                    } else if (kHttpToken[c] == 0) {
                        /*
                         * 1. Forbid empty header name (RFC2616 §2.2)
                         * 2. Forbid line folding (RFC7230 §3.2.4)
                         */
                        return BAD_MSG;
                    }
                    header.start = index;
                    parserState = NAME;
                    break;

                case NAME:
                    for (; ; ) {
                        if (c == ':') {
                            header.end = index;
                            parserState = COLON;
                            break;
                        } else if (kHttpToken[c] == 0) {
                            return BAD_MSG;
                        }
                        if (++index == n) {break;}
                        c = p[index] & 0xff;
                    }
                    break;

                case COLON:
                    if (c == ' ' || c == '\t') {break;}
                    lookup = index;
                    parserState = VALUE;
                    /* fallthrough */
                case VALUE:
                    for (; ; ) {
                        if (c == '\r' || c == '\n') {
                            i = index;
                            while (i > lookup && (p[i - 1] == ' ' || p[i - 1] == '\t')) {
                                --i;
                            }
                            headers.add(new HttpHeader(p, new Slice(header), new Slice(lookup, i)));
                            parserState = c == '\r' ? CR : LF1;
                            break;
                        } else if ((c < 0x20 && c != '\t') || (0x7F <= c && c < 0xA0)) {
                            return BAD_MSG;
                        }
                        if (++index == n) {break;}
                        c = p[index] & 0xff;
                    }
                    break;

                case LF2:
                    if (c == '\n') {
                        return success(new Continue(++index));
                    }
                    return BAD_MSG;
            }
        }
        if (index < LIMIT) {
            return DONE;
        } else {
            return BAD_MSG;
        }
    }

    enum HttpParserState {
        START,
        METHOD,
        URI,
        VERSION,
        STATUS,
        MESSAGE,
        NAME,
        COLON,
        VALUE,
        CR,
        LF1,
        LF2,
    }

    public static final class Slice {
        private int start;
        private int end;

        public Slice(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public Slice(Slice other) {
            this(other.start, other.end);
        }

        public String text(byte[] data) {
            return new String(data, start, end - start, StandardCharsets.ISO_8859_1);
        }

        public int len() {
            return end - start;
        }
    }

    public static final class HttpHeader {
        private final HttpHeaderName headerName;
        private final byte[] source;
        private final Slice nameSlice;
        private final Slice valueSlice;

        public HttpHeader(byte[] source, Slice nameSlice, Slice valueSlice) {
            this.source = source;
            this.nameSlice = nameSlice;
            this.valueSlice = valueSlice;
            this.headerName = StandardHttpHeaderNames.lookup(source, nameSlice.start, nameSlice.len());
        }

        public String text() {
            return nameSlice.text(source) + ": " + valueSlice.text(source);
        }

        public HttpHeaderName name() {
            return headerName;
        }
    }
}

