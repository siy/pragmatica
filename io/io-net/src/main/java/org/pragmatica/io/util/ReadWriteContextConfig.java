package org.pragmatica.io.utils;

import org.pragmatica.io.async.Timeout;
import org.pragmatica.lang.Option;

public record ReadWriteContextConfig(int readBufferSize, int writeBufferSize, Option<Timeout> readTimeout, Option<Timeout> writeTimeout) {}
