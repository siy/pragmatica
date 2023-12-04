package org.pragmatica.io.net.spi;

public interface Transport {
    ClientFactory clientFactory();
    ServerFactory serverFactory();

}
