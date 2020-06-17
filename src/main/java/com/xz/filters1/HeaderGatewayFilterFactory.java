package com.xz.filters1;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class HeaderGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> implements Ordered {
    @Override
    public GatewayFilter apply(Object config) {
        return ((exchange, chain) -> {
            HttpHeaders headers = exchange.getRequest().getHeaders();
            String sessionTokenId = headers.getFirst("sessionTokenId");
            if (StringUtils.isEmpty(sessionTokenId)) {
                log.info("sessionTokenId为空,非法请求");
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
            return chain.filter(exchange);
        });

    }

    @Override
    public int getOrder() {
        return 0;
    }
}
