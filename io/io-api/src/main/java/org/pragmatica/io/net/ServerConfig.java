package org.pragmatica.io.net;

import org.pragmatica.io.net.inet.InetAddress;
import org.pragmatica.io.net.inet.InetPort;
import org.pragmatica.lang.Option;

import static org.pragmatica.lang.Option.none;

public interface ServerConfig {
    InetPort port();

    TransportType transportType();

    Option<InetAddress> address();

    Option<Integer> backlog();

    Option<Integer> sendBufferSize();

    Option<Integer> receiveBufferSize();

    Option<ServerSslConfigName> sslConfig();

    ServerConfig withAddress(InetAddress address);

    ServerConfig withBacklog(int backlog);

    ServerConfig withSendBufferSize(int sendBufferSize);

    ServerConfig withReceiveBufferSize(int receiveBufferSize);

    ServerConfig withSslConfig(ServerSslConfigName sslConfig);

    static ServerConfig defaultConfig(InetPort inetPort, TransportType transportType) {
        record defaultConfig(InetPort port, TransportType transportType, Option<InetAddress> address, Option<Integer> backlog,
                             Option<Integer> sendBufferSize, Option<Integer> receiveBufferSize, Option<ServerSslConfigName> sslConfig)
            implements ServerConfig {
            @Override
            public ServerConfig withAddress(InetAddress address) {
                return new defaultConfig(port, transportType, Option.some(address), backlog, sendBufferSize, receiveBufferSize, sslConfig);
            }

            @Override
            public ServerConfig withBacklog(int backlog) {
                return new defaultConfig(port, transportType, address, Option.some(backlog), sendBufferSize, receiveBufferSize, sslConfig);
            }

            @Override
            public ServerConfig withSendBufferSize(int sendBufferSize) {
                return new defaultConfig(port, transportType, address, backlog, Option.some(sendBufferSize), receiveBufferSize, sslConfig);
            }

            @Override
            public ServerConfig withReceiveBufferSize(int receiveBufferSize) {
                return new defaultConfig(port, transportType, address, backlog, sendBufferSize, Option.some(receiveBufferSize), sslConfig);
            }

            @Override
            public ServerConfig withSslConfig(ServerSslConfigName sslConfig) {
                return new defaultConfig(port, transportType, address, backlog, sendBufferSize, receiveBufferSize, Option.some(sslConfig));
            }
        }

        return new defaultConfig(inetPort, transportType, none(), none(), none(), none(), none());
    }
}
