package org.pragmatica.io.net;

public enum ClientSslConfigurations implements ClientSslConfigName {
    DEFAULT("default");

    private final String configName;

    ClientSslConfigurations(String configName) {
        this.configName = configName;
    }

    @Override
    public String configName() {
        return configName;
    }
}
