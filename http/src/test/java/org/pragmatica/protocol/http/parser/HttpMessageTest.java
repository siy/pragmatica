package org.pragmatica.protocol.http.parser;

import org.junit.jupiter.api.Test;
import org.pragmatica.lang.Result;
import org.pragmatica.protocol.http.parser.ParsingState.Continue;
import org.pragmatica.protocol.http.parser.ParsingState.Done;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.pragmatica.lang.Result.success;

class HttpMessageTest {
    @Test
    void getWithoutBodyParsedSuccessfully() {
        var request =
            """
            GET /hello.htm HTTP/1.1
            User-Agent: Mozilla/4.0 (compatible; MSIE5.01; Windows NT)
            Host: www.tutorialspoint.com
            Accept-Language: en-us
            Accept-Encoding: gzip, deflate
            Connection: Keep-Alive
            """;

        parseRequest(request, success(new Done()));
    }

    @Test
    void postWithBodyParsedSuccessfully() {
        var request =
            """
            POST /cgi-bin/process.cgi HTTP/1.1
            User-Agent: Mozilla/4.0 (compatible; MSIE5.01; Windows NT)
            Host: www.tutorialspoint.com
            Content-Type: application/x-www-form-urlencoded
            Content-Length: length
            Accept-Language: en-us
            Accept-Encoding: gzip, deflate
            Connection: Keep-Alive
                        
            licenseID=string&content=string&/paramsXML=string
            """;

        parseRequest(request, success(new Continue(272)));
    }

    void parseRequest(String request, Result<ParsingState> expected) {
        var message = HttpMessage.forRequest();

        assertEquals(expected, message.parse(request.getBytes(StandardCharsets.ISO_8859_1)));

        System.out.println(message.text());
    }
}