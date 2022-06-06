package org.pragmatica.protocol.http.parser;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HttpParserTest {
    @Test
    void simpleRequestIsParsedSuccessfully() {
        var message = HttpParser.HttpMessage.forRequest();
        var buf =
"""
GET /hello.htm HTTP/1.1
User-Agent: Mozilla/4.0 (compatible; MSIE5.01; Windows NT)
Host: www.tutorialspoint.com
Accept-Language: en-us
Accept-Encoding: gzip, deflate
Connection: Keep-Alive
""";

        var bytes = buf.getBytes(StandardCharsets.ISO_8859_1);
        var rc = HttpParser.parse(message, bytes);

        assertEquals(0, rc);

        System.out.println(message.text(bytes));
    }
}