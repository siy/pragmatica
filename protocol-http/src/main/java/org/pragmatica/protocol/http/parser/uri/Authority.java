package org.pragmatica.protocol.http.parser.uri;

import org.pragmatica.io.async.net.InetPort;
import org.pragmatica.lang.Option;

public record Authority(String hostname, Option<InetPort> port, Option<UserInfo> userInfo) {}
