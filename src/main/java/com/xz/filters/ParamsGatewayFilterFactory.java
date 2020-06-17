package com.xz.filters;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class ParamsGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> implements Ordered {
    @Override
    public GatewayFilter apply(Object config) {
        return ((exchange, chain) -> {
            MultiValueMap<String, String> queryParams = exchange.getRequest().getQueryParams();
            String sessionTokenId = queryParams.getFirst("merchantId");
            if (StringUtils.isEmpty(sessionTokenId)) {
                log.info("merchantId为空,非法请求");
                exchange.getResponse().setStatusCode(HttpStatus.NOT_ACCEPTABLE);
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
