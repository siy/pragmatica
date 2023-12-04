package org.pragmatica.io.net.spi;

import org.pragmatica.io.net.Server;
import org.pragmatica.io.net.ServerConfig;

public interface ServerFactory {
    Server create(ServerConfig config);
}
