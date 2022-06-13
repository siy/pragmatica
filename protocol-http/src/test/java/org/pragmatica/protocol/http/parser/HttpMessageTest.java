package org.pragmatica.protocol.http.parser;

import org.junit.jupiter.api.Test;
import org.pragmatica.io.async.file.FileDescriptor;
import org.pragmatica.io.async.net.*;
import org.pragmatica.io.async.util.OffHeapSlice;
import org.pragmatica.lang.Result;
import org.pragmatica.protocol.http.parser.ParsingResult.Continue;
import org.pragmatica.protocol.http.parser.ParsingResult.Done;

import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.pragmatica.lang.Option.option;
import static org.pragmatica.lang.PromiseIO.*;
import static org.pragmatica.lang.Result.success;

class HttpMessageTest {

    private static final String MINIMAL_REQUEST = "GET / HTTP/1.1\r\nHost: %s\r\nConnection: close\r\n\r\n";

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
    void externalHostCanBeConnectedAndRead() throws UnknownHostException {
        connectAndPrint("www.ibm.com");
    }

    private void connectAndPrint(String host) throws UnknownHostException {
        var addr = java.net.Inet4Address.getByName(host);
        var address = InetAddress.inet4Address(addr.getAddress())
                                 .map(inetAddress -> SocketAddress.socketAddress(InetPort.inetPort(80), inetAddress))
                                 .unwrap();

        System.out.println("Address: " + address);

        var socket = new AtomicReference<FileDescriptor>();

        try (var preparedText = OffHeapSlice.fromBytes(minimalRequest(host));
             var buffer = OffHeapSlice.fixedSize(4096)) {

            var message = HttpMessage.forResponse();

            var parsingResult = socket(AddressFamily.INET, SocketType.STREAM, SocketFlag.closeOnExec(), SocketOption.reuseAll())
                .onSuccess(socket::set)
                .onSuccess(System.out::println)
                .flatMap(fd -> connect(fd, address))
                .flatMap(fd -> write(fd, preparedText))
                .flatMap(__ -> read(socket.get(), buffer))
                .join()
                .onSuccess(__ -> System.out.println("Buffer: " + buffer))
                .flatMap(__ -> message.parse(buffer.export()));

            System.out.println("Parsing result: " + parsingResult + "\n\n");
            System.out.println(message.text());

            assertTrue(parsingResult.isSuccess());
        } finally {
            option(socket.get())
                .onPresent(fd -> close(fd).join());
        }
    }

    private byte[] minimalRequest(String host) {
        return String.format(MINIMAL_REQUEST, host).getBytes(StandardCharsets.US_ASCII);
    }

    void parseRequest(String request, Result<ParsingResult> expected) {
        var message = HttpMessage.forRequest();

        assertEquals(expected, message.parse(request.getBytes(StandardCharsets.ISO_8859_1)));

        System.out.println(message.text());
    }
}