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

import org.pragmatica.lang.Cause;
import org.pragmatica.lang.Causes;
import org.pragmatica.lang.Result;
import org.pragmatica.protocol.http.parser.ParsingState.Done;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.pragmatica.lang.Result.success;

public class ParserConstants {
    public static final Cause INVALID_REQUEST = Causes.cause("Invalid request");
    static final int SHRT_MAX = 0x7fff;
    static final int LIMIT = SHRT_MAX;
    static final Result<ParsingState> BAD_MSG = INVALID_REQUEST.result();
    static final Result<ParsingState> DONE = success(new Done());

    static HttpMethod GetHttpMethod(byte[] p, int offset, int len) {
        if (len < 0) {
            throw new IllegalStateException();
        }

        //TODO: SLOW!!!!
        var str = new String(p, offset, len, StandardCharsets.ISO_8859_1).toUpperCase(Locale.US);

        return HttpMethod.valueOf(str);
    }

    static boolean isDigit(byte b) {
        return kHttpDigit[b] == 1;
    }

    static long READ64BE(byte[] bytes, int i) {
        return ((long) bytes[i + 0] & 0xFF) << 56
           | ((long) bytes[i + 1] & 0xFF) << 48
           | ((long) bytes[i + 2] & 0xFF) << 40
           | ((long) bytes[i + 3] & 0xFF) << 32
           | ((long) bytes[i + 4] & 0xFF) << 24
           | ((long) bytes[i + 5] & 0xFF) << 16
           | ((long) bytes[i + 6] & 0xFF) << 8
           | ((long) bytes[i + 7] & 0xFF);
    }

    private static final int[] DOWN_CASE_TABLE = {
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14,
        15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29,
        30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40, 41, 42, 43, 44,
        45, 46, 47, 48, 49, 50, 51, 52, 53, 54, 55, 56, 57, 58, 59,
        60, 61, 62, 63, 64, 97, 98, 99, 100, 101, 102, 103, 104, 105, 106,
        107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119, 120, 121,
        122, 91, 92, 93, 94, 95, 96, 97, 98, 99, 100, 101, 102, 103, 104,
        105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115, 116, 117, 118, 119,
        120, 121, 122, 123, 124, 125, 126, 127, 128, 129, 130, 131, 132, 133, 134,
        135, 136, 137, 138, 139, 140, 141, 142, 143, 144, 145, 146, 147, 148, 149,
        150, 151, 152, 153, 154, 155, 156, 157, 158, 159, 160, 161, 162, 163, 164,
        165, 166, 167, 168, 169, 170, 171, 172, 173, 174, 175, 176, 177, 178, 179,
        180, 181, 182, 183, 184, 185, 186, 187, 188, 189, 190, 191, 192, 193, 194,
        195, 196, 197, 198, 199, 200, 201, 202, 203, 204, 205, 206, 207, 208, 209,
        210, 211, 212, 213, 214, 215, 216, 217, 218, 219, 220, 221, 222, 223, 224,
        225, 226, 227, 228, 229, 230, 231, 232, 233, 234, 235, 236, 237, 238, 239,
        240, 241, 242, 243, 244, 245, 246, 247, 248, 249, 250, 251, 252, 253, 254,
        255
    };

    static int compare(byte[] s1, int offset, byte[] s2, int len) {
        for (int n = 0; n < len; n++) {
            var cmp = DOWN_CASE_TABLE[s1[n + offset] & 0xFF] - DOWN_CASE_TABLE[s2[n] & 0xFF];

            if (cmp == 0) {
                continue;
            }

            return cmp;
        }
        return 0;
    }



    public static final int kHttpClientStateHeaders = 0;
    public static final int kHttpClientStateBody = 1;
    public static final int kHttpClientStateBodyChunked = 2;
    public static final int kHttpClientStateBodyLengthed = 3;

    public static final int kHttpStateChunkStart = 0;
    public static final int kHttpStateChunkSize = 1;
    public static final int kHttpStateChunkExt = 2;
    public static final int kHttpStateChunkLf1 = 3;
    public static final int kHttpStateChunk = 4;
    public static final int kHttpStateChunkCr2 = 5;
    public static final int kHttpStateChunkLf2 = 6;
    public static final int kHttpStateTrailerStart = 7;
    public static final int kHttpStateTrailer = 8;
    public static final int kHttpStateTrailerLf1 = 9;
    public static final int kHttpStateTrailerLf2 = 10;

    public static final int kHttpHost = 0;
    public static final int kHttpCacheControl = 1;
    public static final int kHttpConnection = 2;
    public static final int kHttpAccept = 3;
    public static final int kHttpAcceptLanguage = 4;
    public static final int kHttpAcceptEncoding = 5;
    public static final int kHttpUserAgent = 6;
    public static final int kHttpReferer = 7;
    public static final int kHttpXForwardedFor = 8;
    public static final int kHttpOrigin = 9;
    public static final int kHttpUpgradeInsecureRequests = 10;
    public static final int kHttpPragma = 11;
    public static final int kHttpCookie = 12;
    public static final int kHttpDnt = 13;
    public static final int kHttpSecGpc = 14;
    public static final int kHttpFrom = 15;
    public static final int kHttpIfModifiedSince = 16;
    public static final int kHttpXRequestedWith = 17;
    public static final int kHttpXForwardedHost = 18;
    public static final int kHttpXForwardedProto = 19;
    public static final int kHttpXCsrfToken = 20;
    public static final int kHttpSaveData = 21;
    public static final int kHttpRange = 22;
    public static final int kHttpContentLength = 23;
    public static final int kHttpContentType = 24;
    public static final int kHttpVary = 25;
    public static final int kHttpDate = 26;
    public static final int kHttpServer = 27;
    public static final int kHttpExpires = 28;
    public static final int kHttpContentEncoding = 29;
    public static final int kHttpLastModified = 30;
    public static final int kHttpEtag = 31;
    public static final int kHttpAllow = 32;
    public static final int kHttpContentRange = 33;
    public static final int kHttpAcceptCharset = 34;
    public static final int kHttpAccessControlAllowCredentials = 35;
    public static final int kHttpAccessControlAllowHeaders = 36;
    public static final int kHttpAccessControlAllowMethods = 37;
    public static final int kHttpAccessControlAllowOrigin = 38;
    public static final int kHttpAccessControlMaxAge = 39;
    public static final int kHttpAccessControlMethod = 40;
    public static final int kHttpAccessControlRequestHeaders = 41;
    public static final int kHttpAccessControlRequestMethod = 42;
    public static final int kHttpAccessControlRequestMethods = 43;
    public static final int kHttpAge = 44;
    public static final int kHttpAuthorization = 45;
    public static final int kHttpContentBase = 46;
    public static final int kHttpContentDescription = 47;
    public static final int kHttpContentDisposition = 48;
    public static final int kHttpContentLanguage = 49;
    public static final int kHttpContentLocation = 50;
    public static final int kHttpContentMd5 = 51;
    public static final int kHttpExpect = 52;
    public static final int kHttpIfMatch = 53;
    public static final int kHttpIfNoneMatch = 54;
    public static final int kHttpIfRange = 55;
    public static final int kHttpIfUnmodifiedSince = 56;
    public static final int kHttpKeepAlive = 57;
    public static final int kHttpLink = 58;
    public static final int kHttpLocation = 59;
    public static final int kHttpMaxForwards = 60;
    public static final int kHttpProxyAuthenticate = 61;
    public static final int kHttpProxyAuthorization = 62;
    public static final int kHttpProxyConnection = 63;
    public static final int kHttpPublic = 64;
    public static final int kHttpRetryAfter = 65;
    public static final int kHttpTe = 66;
    public static final int kHttpTrailer = 67;
    public static final int kHttpTransferEncoding = 68;
    public static final int kHttpUpgrade = 69;
    public static final int kHttpWarning = 70;
    public static final int kHttpWwwAuthenticate = 71;
    public static final int kHttpVia = 72;
    public static final int kHttpStrictTransportSecurity = 73;
    public static final int kHttpXFrameOptions = 74;
    public static final int kHttpXContentTypeOptions = 75;
    public static final int kHttpAltSvc = 76;
    public static final int kHttpReferrerPolicy = 77;
    public static final int kHttpXXssProtection = 78;
    public static final int kHttpAcceptRanges = 79;
    public static final int kHttpSetCookie = 80;
    public static final int kHttpSecChUa = 81;
    public static final int kHttpSecChUaMobile = 82;
    public static final int kHttpSecFetchSite = 83;
    public static final int kHttpSecFetchMode = 84;
    public static final int kHttpSecFetchUser = 85;
    public static final int kHttpSecFetchDest = 86;
    public static final int kHttpHeadersMax = 87;


    public static final byte kHttpToken[] = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0x00
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0x10
        0, 1, 0, 1, 1, 1, 1, 1, 0, 0, 1, 1, 0, 1, 1, 0, // 0x20
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, // 0x30
        0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, // 0x40
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 1, 1, // 0x50
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, // 0x60
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1, 0, 1, 0, // 0x70
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0x80
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0x90
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0xa0
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0xb0
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0xc0
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0xd0
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0xe0
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0xf0
    };

    public static final byte kHttpDigit[] = {
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0x00
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0x10
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0x20
        1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0, 0, 0, 0, 0, // 0x30
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0x40
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0x50
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0x60
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0x70
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0x80
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0x90
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0xa0
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0xb0
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0xc0
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0xd0
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0xe0
        0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, // 0xf0
    };

    public static final boolean[] kHttpRepeatable = new boolean[kHttpHeadersMax];

    static {
        kHttpRepeatable[kHttpAcceptCharset] = true;
        kHttpRepeatable[kHttpAcceptEncoding] = true;
        kHttpRepeatable[kHttpAcceptLanguage] = true;
        kHttpRepeatable[kHttpAccept] = true;
        kHttpRepeatable[kHttpAllow] = true;
        kHttpRepeatable[kHttpCacheControl] = true;
        kHttpRepeatable[kHttpContentEncoding] = true;
        kHttpRepeatable[kHttpContentLanguage] = true;
        kHttpRepeatable[kHttpExpect] = true;
        kHttpRepeatable[kHttpIfMatch] = true;
        kHttpRepeatable[kHttpIfNoneMatch] = true;
        kHttpRepeatable[kHttpPragma] = true;
        kHttpRepeatable[kHttpProxyAuthenticate] = true;
        kHttpRepeatable[kHttpPublic] = true;
        kHttpRepeatable[kHttpTe] = true;
        kHttpRepeatable[kHttpTrailer] = true;
        kHttpRepeatable[kHttpTransferEncoding] = true;
        kHttpRepeatable[kHttpUpgrade] = true;
        kHttpRepeatable[kHttpVary] = true;
        kHttpRepeatable[kHttpVia] = true;
        kHttpRepeatable[kHttpWarning] = true;
        kHttpRepeatable[kHttpWwwAuthenticate] = true;
        kHttpRepeatable[kHttpXForwardedFor] = true;
        kHttpRepeatable[kHttpAccessControlAllowHeaders] = true;
        kHttpRepeatable[kHttpAccessControlAllowMethods] = true;
        kHttpRepeatable[kHttpAccessControlRequestHeaders] = true;
        kHttpRepeatable[kHttpAccessControlRequestMethods] = true;
    }

    ;

}
