package org.pragmatica.protocol.http.parser;

import org.junit.jupiter.api.Test;
import org.pragmatica.dns.DomainNameResolver;
import org.pragmatica.io.async.net.InetAddress;
import org.pragmatica.io.async.net.InetPort;
import org.pragmatica.io.async.util.SliceAccessor;
import org.pragmatica.io.util.ClientConnector;
import org.pragmatica.io.util.ReadWriteContext;
import org.pragmatica.lang.Promise;
import org.pragmatica.lang.Result;
import org.pragmatica.protocol.http.parser.ParsingResult.Continue;
import org.pragmatica.protocol.http.parser.ParsingResult.Done;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.pragmatica.lang.Result.success;

class HttpMessageTest {

    private static final String MINIMAL_REQUEST = "GET / HTTP/1.1\r\nHost: %s\r\nConnection: close\r\n\r\n";

    private final DomainNameResolver resolver = DomainNameResolver.resolver();

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

    @Test
    void externalHostCanBeConnectedAndRead() {
        writeReadHost("www.google.com");
    }

    private void writeReadHost(String host) {
        var result =
            resolver.forName(host)
                    .map(domainAddress -> domainAddress.clientTcpConnector(InetPort.inetPort(80)))
                    .flatMap(ClientConnector::connect)
                    .map(ReadWriteContext::readWriteContext)
                    .flatMap(context -> doConversation(context, host))
                    .flatMap(ReadWriteContext::close)
                    .join();

        System.out.println("\nResult: " + result + "\n");
    }

    private <T extends InetAddress> Promise<ReadWriteContext<T>> doConversation(ReadWriteContext<T> context, String host) {
        var request = String.format(MINIMAL_REQUEST, host).getBytes(StandardCharsets.UTF_8);
        var message = HttpMessage.forResponse();

        return context.exchangeSimple(writeAccessor -> writeAccessor.putBytes(request).updateSlice(), message::parse)
                      .map(SliceAccessor::remainingAsString)
                      .onSuccessDo(() -> System.out.println(message.text()))
                      .onSuccess(body -> System.out.println("Body:\n" + body + "\n"))
                      .onFailure(System.out::println)
                      .map(() -> context);
    }

    void parseRequest(String request, Result<ParsingResult> expected) {
        var message = HttpMessage.forRequest();

        assertEquals(expected, message.parse(request.getBytes(StandardCharsets.ISO_8859_1)));

        System.out.println(message.text());
    }
}