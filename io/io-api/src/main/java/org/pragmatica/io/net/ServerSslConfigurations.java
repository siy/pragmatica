package org.pragmatica.io.net;

public enum ServerSslConfigurations implements ServerSslConfigName {
    SELF_SIGNED("self-signed");

    private final String configName;

    ServerSslConfigurations(String configName) {
        this.configName = configName;
    }

    @Override
    public String configName() {
        return configName;
    }
}
