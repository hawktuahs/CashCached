package com.bt.main.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CorsHeaderStripFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            ServerHttpResponse response = exchange.getResponse();
            HttpHeaders headers = response.getHeaders();

            if (headers.containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)) {
                headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN);
            }
            if (headers.containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS)) {
                headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_METHODS);
            }
            if (headers.containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS)) {
                headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS);
            }
            if (headers.containsKey(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS)) {
                headers.remove(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS);
            }
            if (headers.containsKey(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS)) {
                headers.remove(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS);
            }
            if (headers.containsKey(HttpHeaders.ACCESS_CONTROL_MAX_AGE)) {
                headers.remove(HttpHeaders.ACCESS_CONTROL_MAX_AGE);
            }
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
