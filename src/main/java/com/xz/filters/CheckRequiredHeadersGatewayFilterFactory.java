package com.xz.filters;

import com.xz.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.Optional;


/**
 * 将必要的信息放入header
 */
@Slf4j
@Component
public class CheckRequiredHeadersGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {
    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            HttpHeaders headers = exchange.getRequest().getHeaders();
            String signature = headers.getFirst("signature");
            if (StringUtils.isEmpty(signature))
                throw new CustomException(HttpStatus.BAD_REQUEST, "header-signature must not be null");
            exchange.getAttributes().put("signature", signature);
            return chain.filter(exchange);
        };
    }
}