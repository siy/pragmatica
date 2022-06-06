package org.pragmatica.protocol.http.parser;

public class ParserConstants {
    public static final int SHRT_MAX = 0x7fff;
    public static final int LIMIT = SHRT_MAX;

    enum ParserType{
        REQUEST,
        RESPONSE,
    }

    public static final int kHttpGet = 1;
    public static final int kHttpHead = 2;
    public static final int kHttpPost = 3;
    public static final int kHttpPut = 4;
    public static final int kHttpDelete = 5;
    public static final int kHttpOptions = 6;
    public static final int kHttpConnect = 7;
    public static final int kHttpTrace = 8;
    public static final int kHttpCopy = 9;
    public static final int kHttpLock = 10;
    public static final int kHttpMerge = 11;
    public static final int kHttpMkcol = 12;
    public static final int kHttpMove = 13;
    public static final int kHttpNotify = 14;
    public static final int kHttpPatch = 15;
    public static final int kHttpReport = 16;
    public static final int kHttpUnlock = 17;

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
