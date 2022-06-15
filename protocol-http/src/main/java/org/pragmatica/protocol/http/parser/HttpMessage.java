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

import org.pragmatica.io.async.util.SliceAccessor;
import org.pragmatica.lang.Option.Some;
import org.pragmatica.lang.Result;
import org.pragmatica.protocol.http.parser.ParsingResult.Continue;
import org.pragmatica.protocol.http.parser.header.HttpHeader;
import org.pragmatica.protocol.http.parser.header.StandardHttpHeaderNames;
import org.pragmatica.protocol.http.parser.util.DetachedSlice;
import org.pragmatica.protocol.http.parser.util.Slice;

import java.util.ArrayList;
import java.util.List;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.pragmatica.lang.Result.success;
import static org.pragmatica.protocol.http.parser.HttpMessage.HttpParserState.*;
import static org.pragmatica.protocol.http.parser.ParserType.REQUEST;
import static org.pragmatica.protocol.http.parser.ParsingErrors.*;
import static org.pragmatica.protocol.http.parser.util.ParserHelper.*;

public class HttpMessage {
    private static final int LIMIT = 0x7fff;
    private static final Result<ParsingResult> DONE = success(new ParsingResult.Done());
    private byte[] source;

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

    private int index;
    private int lookup;
    private int status;
    private HttpParserState parserState = START;
    private HttpMethod method;
    private int version;
    private final DetachedSlice uri = new DetachedSlice();
    private final DetachedSlice message = new DetachedSlice();
    private final ParserType type;
    private final DetachedSlice header = new DetachedSlice();
    private final List<HttpHeader> headers = new ArrayList<>();

    private HttpMessage(ParserType type) {
        this.type = type;
    }

    public static HttpMessage forRequest() {
        return new HttpMessage(REQUEST);
    }

    public static HttpMessage forResponse() {
        return new HttpMessage(ParserType.RESPONSE);
    }

    public List<HttpHeader> headers() {
        return headers;
    }

    public String text() {
        var builder = new StringBuilder().append("Protocol Version: ").append(version).append('\n');

        if (type == REQUEST) {
            builder.append("Method: ").append(method).append('\n')
                   .append("URI: ").append(uri.text(source, UTF_8)).append('\n');
        } else {
            builder.append("Status: ").append(status).append('\n')
                   .append("Message: ").append(message.text(source, UTF_8)).append('\n');
        }

        builder
            .append("Headers (").append(headers.size()).append("):").append('\n');

        for (var h : headers) {
            builder.append('\t').append(h).append('\n');
        }

        return builder.toString();
    }

    public Result<SliceAccessor> parse(SliceAccessor sliceAccessor) {
        return parse(sliceAccessor.getRemainingBytes())
            .map(result -> result.bodyPosition(sliceAccessor));
    }

    public Result<ParsingResult> parse(byte[] input) {
        if (parserState != START) {
            throw new IllegalStateException("HttpMessage.parse() can be invoked only once");
        }

        int c, i;
        int n = input.length;
        this.source = input;

        for (; index < n; ++index) {
            c = input[index] & 0xff;

            switch (parserState) {
                case START:
                    if (c == '\r' || c == '\n') {
                        break; /* RFC7230 § 3.5 */
                    }

                    if (isNotToken(c)) {
                        return INVALID_REQUEST_HEADER.result();
                    }
                    parserState = type == REQUEST ? METHOD : VERSION;
                    lookup = index;

                    break;
                case METHOD:
                    for (; ; ) {
                        if (c == ' ') {
                            var httpMethod = HttpMethod.lookup(input, lookup, index - lookup);

                            if (httpMethod instanceof Some<HttpMethod> some) {
                                method = some.value();
                            } else {
                                return UNKNOWN_METHOD.result();
                            }

                            lookup = index + 1;
                            parserState = URI;
                            break;
                        } else if (isNotToken(c)) {
                            return INVALID_REQUEST_HEADER.result();
                        }
                        if (++index == n) {
                            break;
                        }
                        c = input[index] & 0xff;
                    }
                    break;
                case URI:
                    for (; ; ) {
                        if (c == ' ' || c == '\r' || c == '\n') {
                            if (index == lookup) {
                                return INVALID_URI.result();
                            }

                            uri.start(lookup);
                            uri.end(index);

                            if (c == ' ') {
                                lookup = index + 1;
                                parserState = VERSION;
                            } else {
                                version = 9;
                                parserState = c == '\r' ? CR : LF1;
                            }
                            break;
                        } else if (c < 0x20 || (0x7F <= c && c < 0xA0)) {
                            return INVALID_CHARACTER_IN_HEADER.result();
                        }
                        if (++index == n) {
                            break;
                        }
                        c = input[index] & 0xff;
                    }
                    break;
                case VERSION:
                    if (c == ' ' || c == '\r' || c == '\n') {
                        if (index - lookup == 8 &&
                            (READ64BE(input, lookup) & 0xFFFFFFFFFF00FF00L) == 0x485454502F002E00L
                            && isDigit(input[lookup + 5])
                            && isDigit(input[lookup + 7])) {

                            version = (input[lookup + 5] - '0') * 10 + (input[lookup + 7] - '0');

                            if (type == REQUEST) {
                                parserState = c == '\r' ? CR : LF1;
                            } else {
                                parserState = STATUS;
                            }
                        } else {
                            return INVALID_HTTP_VERSION.result();
                        }
                    }
                    break;
                case STATUS:
                    for (; ; ) {
                        if (c == ' ' || c == '\r' || c == '\n') {
                            if (status < 100) {
                                return INVALID_STATUS_CODE.result();
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
                                return INVALID_STATUS_CODE.result();
                            }
                        } else {
                            return INVALID_STATUS_CODE.result();
                        }
                        if (++index == n) {
                            break;
                        }
                        c = input[index] & 0xff;
                    }
                    break;

                case MESSAGE:
                    for (; ; ) {
                        if (c == '\r' || c == '\n') {
                            message.start(lookup);
                            message.end(index);
                            parserState = c == '\r' ? CR : LF1;
                            break;
                        } else if (c < 0x20 || (0x7F <= c && c < 0xA0)) {
                            return INVALID_CHARACTER_IN_HEADER.result();
                        }
                        if (++index == n) {
                            break;
                        }
                        c = input[index] & 0xff;
                    }
                    break;

                case CR:
                    if (c != '\n') {
                        return INVALID_REQUEST_HEADER.result();
                    }
                    parserState = LF1;
                    break;

                case LF1:
                    if (c == '\r') {
                        parserState = LF2;
                        break;
                    } else if (c == '\n') {
                        return success(new Continue(++index));
                    } else if (isNotToken(c)) {
                        /*
                         * 1. Forbid empty header name (RFC2616 §2.2)
                         * 2. Forbid line folding (RFC7230 §3.2.4)
                         */
                        return INVALID_REQUEST_HEADER.result();
                    }
                    header.start(index);
                    parserState = NAME;
                    break;

                case NAME:
                    for (; ; ) {
                        if (c == ':') {
                            header.end(index);
                            parserState = COLON;
                            break;
                        } else if (isNotToken(c)) {
                            return INVALID_REQUEST_HEADER_NAME.result();
                        }
                        if (++index == n) {
                            break;
                        }
                        c = input[index] & 0xff;
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
                            while (i > lookup && (input[i - 1] == ' ' || input[i - 1] == '\t')) {
                                --i;
                            }

                            var headerName = StandardHttpHeaderNames.lookup(input, header.start(), header.len());
                            headers.add(HttpHeader.createParsed(headerName, Slice.fromBytes(input, lookup, i)));
                            parserState = c == '\r' ? CR : LF1;
                            break;
                        } else if ((c < 0x20 && c != '\t') || (0x7F <= c && c < 0xA0)) {
                            return INVALID_REQUEST_HEADER_VALUE.result();
                        }
                        if (++index == n) {
                            break;
                        }
                        c = input[index] & 0xff;
                    }
                    break;

                case LF2:
                    if (c == '\n') {
                        return success(new Continue(++index));
                    }
                    return INVALID_REQUEST_HEADER.result();
            }
        }
        if (index < LIMIT) {
            return DONE;
        } else {
            return REQUEST_HEADER_TOO_LONG.result();
        }
    }
}

