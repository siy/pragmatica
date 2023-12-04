package org.pragmatica.io.net;

import org.pragmatica.lang.Option;

import static org.pragmatica.lang.Option.none;

public interface ClientConfig {
    TransportType transportType();

    Option<Integer> sendBufferSize();

    Option<Integer> receiveBufferSize();

    Option<ClientSslConfigName> sslConfig();

    ClientConfig withSendBufferSize(int sendBufferSize);

    ClientConfig withReceiveBufferSize(int receiveBufferSize);

    ClientConfig withSslConfig(ClientSslConfigName sslConfig);

    static ClientConfig defaultConfig(TransportType transportType) {
        record defaultConfig(
            TransportType transportType,
            Option<Integer> sendBufferSize,
            Option<Integer> receiveBufferSize,
            Option<ClientSslConfigName> sslConfig
        ) implements ClientConfig {
            @Override
            public ClientConfig withSendBufferSize(int sendBufferSize) {
                return new defaultConfig(
                    transportType,
                    Option.some(sendBufferSize),
                    receiveBufferSize,
                    sslConfig
                );
            }

            @Override
            public ClientConfig withReceiveBufferSize(int receiveBufferSize) {
                return new defaultConfig(
                    transportType,
                    sendBufferSize,
                    Option.some(receiveBufferSize),
                    sslConfig
                );
            }

            @Override
            public ClientConfig withSslConfig(ClientSslConfigName sslConfig) {
                return new defaultConfig(
                    transportType,
                    sendBufferSize,
                    receiveBufferSize,
                    Option.some(sslConfig)
                );
            }
        }

        return new defaultConfig(transportType, none(), none(), none());
    }
}
