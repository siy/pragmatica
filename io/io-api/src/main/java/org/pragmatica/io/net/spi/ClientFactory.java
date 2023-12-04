package org.pragmatica.io.net.spi;

import org.pragmatica.io.net.Client;
import org.pragmatica.io.net.ClientConfig;

public interface ClientFactory {
    Client create(ClientConfig config);
}
