/*
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

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.pragmatica.protocol.http.parser.ParserConstants.*;
import static org.pragmatica.protocol.http.parser.ParserConstants.HttpParserState.*;

public class HttpParser {
    public static class Slice {
        int start;
        int end;

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
    }

    public static class SlicePair {
        Slice name;
        Slice value;

        public SlicePair(Slice name, Slice value) {
            this.name = name;
            this.value = value;
        }

        public String text(byte[] data) {
            return name.text(data) + ": " + value.text(data);
        }
    }

    public static class HttpMessage {
        int index;
        int a;
        int status;
        HttpParserState parserState = START;
        ParserType type;
        HttpMethod method;
        int version;
        Slice header = new Slice(0, 0);
        Slice uri = new Slice(0, 0);
        Slice message = new Slice(0, 0);
        List<SlicePair> headers = new ArrayList<>();
        Slice xmethod = new Slice(0, 0);

        private HttpMessage(ParserType type) {
            this.type = type;
        }

        public static HttpMessage forRequest() {
            return new HttpMessage(ParserType.REQUEST);
        }

        public static HttpMessage forResponse() {
            return new HttpMessage(ParserType.RESPONSE);
        }

        public String text(byte[] data) {
            var builder = new StringBuilder();

            builder.append("Method: ").append(method).append("\n")
                .append("Version: ").append(version).append("\n")
                .append("Headers: ").append(headers.size()).append("\n");


            for (var h : headers) {
                builder.append("  ").append(h.text(data)).append("\n");
            }

            return builder.toString();
        }
    }

    /**
     * Parses HTTP request or response.
     * <p>
     * This parser is responsible for determining the length of a message and slicing the strings inside it. Performance is attained using perfect
     * hash tables. No memory allocation is performed for normal messages. Line folding is forbidden. State persists across calls so that fragmented
     * messages can be handled efficiently. A limitation on message size is imposed to make the header data structures smaller.
     * <p>
     * This parser assumes ISO-8859-1 and guarantees no C0 or C1 control codes are present in message fields, with the exception of tab. Please note
     * that fields like kHttpStateUri may use UTF-8 percent encoding. This parser doesn't care if you choose ASA X3.4-1963 or MULTICS newlines.
     * <p>
     * kHttpRepeatable defines which standard header fields are O(1) and which ones may have comma entries spilled over into xheaders. For most
     * headers it's sufficient to simply check the static slice. If r.headers[kHttpFoo].a is zero then the header is totally absent.
     * <p>
     * This parser has linear complexity. Each character only needs to be considered a single time. That's the case even if messages are fragmented.
     * If a message is valid but incomplete, this function will return zero so that it can be resumed as soon as more data arrives.
     * <p>
     * This parser takes about 400 nanoseconds to parse a 403 byte Chrome HTTP request under MODE=rel on a Core i9 which is about three cycles per
     * byte or a gigabyte per second of throughput per core.
     *
     * @note we assume p points to a buffer that has >=SHRT_MAX bytes
     * @see HTTP/1.1 RFC2616 RFC2068
     * @see HTTP/1.0 RFC1945
     */
    public static int parse(HttpMessage r, byte[] p) {
        int c, i;
        int n = p.length;

        for (; r.index < n; ++r.index) {
            c = p[r.index] & 0xff;

            switch (r.parserState) {
                case START:
                    if (c == '\r' || c == '\n') {
                        break; /* RFC7230 § 3.5 */
                    }
                    if (kHttpToken[c] == 0) {
                        return ebadmsg();
                    }
                    r.parserState = r.type == ParserType.REQUEST ? METHOD : VERSION;
                    r.a = r.index;
                    break;
                case METHOD:
                    for (; ; ) {
                        if (c == ' ') {
                            r.method = GetHttpMethod(p, r.a, r.index - r.a);
                            r.xmethod.start = r.a;
                            r.xmethod.end = r.index;
                            r.a = r.index + 1;
                            r.parserState = URI;
                            break;
                        } else if (kHttpToken[c] == 0) {
                            return ebadmsg();
                        }
                        if (++r.index == n) {
                            break;
                        }
                        c = p[r.index] & 0xff;
                    }
                    break;
                case URI:
                    for (; ; ) {
                        if (c == ' ' || c == '\r' || c == '\n') {
                            if (r.index == r.a) {
                                return ebadmsg();
                            }
                            r.uri.start = r.a;
                            r.uri.end = r.index;
                            if (c == ' ') {
                                r.a = r.index + 1;
                                r.parserState = VERSION;
                            } else {
                                r.version = 9;
                                r.parserState = c == '\r' ? CR : LF1;
                            }
                            break;
                        } else if (c < 0x20 || (0x7F <= c && c < 0xA0)) {
                            return ebadmsg();
                        }
                        if (++r.index == n) {break;}
                        c = p[r.index] & 0xff;
                    }
                    break;
                case VERSION:
                    if (c == ' ' || c == '\r' || c == '\n') {
                        if (r.index - r.a == 8 && (READ64BE(p, r.a) & 0xFFFFFFFFFF00FF00L) == 0x485454502F002E00L && isdigit(p[r.a + 5]) && isdigit(p[r.a
                                                                                                                                                      + 7])) {
                            r.version = (p[r.a + 5] - '0') * 10 + (p[r.a + 7] - '0');
                            if (r.type == ParserType.REQUEST) {
                                r.parserState = c == '\r' ? CR : LF1;
                            } else {
                                r.parserState = STATUS;
                            }
                        } else {
                            return ebadmsg();
                        }
                    }
                    break;
                case STATUS:
                    for (; ; ) {
                        if (c == ' ' || c == '\r' || c == '\n') {
                            if (r.status < 100) {return ebadmsg();}
                            if (c == ' ') {
                                r.a = r.index + 1;
                                r.parserState = MESSAGE;
                            } else {
                                r.parserState = c == '\r' ? CR : LF1;
                            }
                            break;
                        } else if ('0' <= c && c <= '9') {
                            r.status *= 10;
                            r.status += c - '0';
                            if (r.status > 999) {return ebadmsg();}
                        } else {
                            return ebadmsg();
                        }
                        if (++r.index == n) {break;}
                        c = p[r.index] & 0xff;
                    }
                    break;
                case MESSAGE:
                    for (; ; ) {
                        if (c == '\r' || c == '\n') {
                            r.message.start = r.a;
                            r.message.end = r.index;
                            r.parserState = c == '\r' ? CR : LF1;
                            break;
                        } else if (c < 0x20 || (0x7F <= c && c < 0xA0)) {
                            return ebadmsg();
                        }
                        if (++r.index == n) {break;}
                        c = p[r.index] & 0xff;
                    }
                    break;
                case CR:
                    if (c != '\n') {return ebadmsg();}
                    r.parserState = LF1;
                    break;
                case LF1:
                    if (c == '\r') {
                        r.parserState = LF2;
                        break;
                    } else if (c == '\n') {
                        return ++r.index;
                    } else if (kHttpToken[c] == 0) {
                        /*
                         * 1. Forbid empty header name (RFC2616 §2.2)
                         * 2. Forbid line folding (RFC7230 §3.2.4)
                         */
                        return ebadmsg();
                    }
                    r.header.start = r.index;
                    r.parserState = NAME;
                    break;
                case NAME:
                    for (; ; ) {
                        if (c == ':') {
                            r.header.end = r.index;
                            r.parserState = COLON;
                            break;
                        } else if (kHttpToken[c] == 0) {
                            return ebadmsg();
                        }
                        if (++r.index == n) {break;}
                        c = p[r.index] & 0xff;
                    }
                    break;
                case COLON:
                    if (c == ' ' || c == '\t') {break;}
                    r.a = r.index;
                    r.parserState = VALUE;
                    /* fallthrough */
                case VALUE:
                    for (; ; ) {
                        if (c == '\r' || c == '\n') {
                            i = r.index;
                            while (i > r.a && (p[i - 1] == ' ' || p[i - 1] == '\t')) {
                                --i;
                            }
                            r.headers.add(new SlicePair(new Slice(r.header), new Slice(r.a, i)));
                            r.parserState = c == '\r' ? CR : LF1;
                            break;
                        } else if ((c < 0x20 && c != '\t') || (0x7F <= c && c < 0xA0)) {
                            return ebadmsg();
                        }
                        if (++r.index == n) {break;}
                        c = p[r.index] & 0xff;
                    }
                    break;
                case LF2:
                    if (c == '\n') {
                        return ++r.index;
                    }
                    return ebadmsg();
                default:
                    throw new UnsupportedOperationException();
            }
        }
        if (r.index < LIMIT) {
            return 0;
        } else {
            return ebadmsg();
        }
    }



    private static HttpMethod GetHttpMethod(byte[] p, int offset, int len) {
        if (len < 0) {
            throw new IllegalStateException();
        }

        //TODO: SLOW!!!!
        var str = new String(p, offset, len, StandardCharsets.ISO_8859_1).toUpperCase(Locale.US);

        return HttpMethod.valueOf(str);
    }

    private static boolean isdigit(byte b) {
        return b >= 0x30 && b <= 0x39;
    }

    private static long READ64BE(byte[] bytes, int i) {
        var value = ((long) bytes[i + 0] & 0xFF) << 56
               | ((long) bytes[i + 1] & 0xFF) << 48
               | ((long) bytes[i + 2] & 0xFF) << 40
               | ((long) bytes[i + 3] & 0xFF) << 32
               | ((long) bytes[i + 4] & 0xFF) << 24
               | ((long) bytes[i + 5] & 0xFF) << 16
               | ((long) bytes[i + 6] & 0xFF) << 8
               | ((long) bytes[i + 7] & 0xFF);

        return value;
    }

    private static int ebadmsg() {
        return -1;
    }
}

