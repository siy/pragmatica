package org.pragmatica.protocol.http.parser.uri;

import org.pragmatica.lang.Option;

public record UserInfo(String userName, Option<String> password) {}
