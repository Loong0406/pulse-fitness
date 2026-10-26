package com.pulse.server;

import com.sun.net.httpserver.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/** Proxy controls access before the actual plan handler is invoked. */
public final class AuthorizedHandlerProxy implements HttpHandler {
    private final HttpHandler target;
    private final String token;

    public AuthorizedHandlerProxy(String token, HttpHandler target) {
        this.token = token;
        this.target = target;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        if (!token.isEmpty()) {
            String value = exchange.getRequestHeaders().getFirst("Authorization");
            if (value == null
                    || !MessageDigest.isEqual(
                            ("Bearer " + token).getBytes(StandardCharsets.UTF_8),
                            value.getBytes(StandardCharsets.UTF_8))) {
                FitnessServer.json(exchange, 401, "{\"error\":\"服务访问令牌无效\"}");
                return;
            }
        }
        target.handle(exchange);
    }
}
