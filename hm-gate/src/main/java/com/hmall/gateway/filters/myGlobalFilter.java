package com.hmall.gateway.filters;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * <p>Project: hmall</p>
 * <p>Date: 2025/8/25 10:31</p>
 * Description:
 */
public class myGlobalFilter implements GlobalFilter, Ordered {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        request.getHeaders().set("Authorization", "Bearer " + request.getHeaders().getFirst("Authorization"));
        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        // 值越小，优先级越高
        return 0;
    }
}
