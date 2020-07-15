package com.xz.filters;

import com.alibaba.fastjson.JSON;
import com.xz.rewrite.MyCachedBodyOutputMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.support.AbstractConfigurable;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.cloud.gateway.support.ServerWebExchangeUtils;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 将EncryptRequestBodyFilter形式修改由GlobalFilter修改为AbstractGatewayFilterFactory形式，这样后面可以为指定的请求设置而不是所有的请求都拦截
 * <p>
 * see  ModifyRequestBodyGatewayFilterFactory
 */
@Slf4j
@Component
public class EncryptRequestBodyGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> implements Ordered {
    @Override
    public GatewayFilter apply(Object config) {
        log.info("enter EncryptRequestBodyGatewayFilterFactory");
        return (exchange, chain) -> {
            Map<String, Object> requestBody = exchange.getAttribute("cachedRequestBodyObject");
            log.info("EncryptRequestBodyGatewayFilterFactory requestBody:{}", JSON.toJSONString(requestBody));

            /**
             * 在初始化路由时可以将 String.format("%s::%s", service.getAlias(), api.getAlias())作为routeId
             * 然后这里进行解析,拿到对应的别名,查询数据库是否存在,不存在则抛出异常
             * 是在RoutePredicateHandlerMapping中的getHandlerInternal的return lookupRoute(exchange)的exchange.getAttributes().put(GATEWAY_ROUTE_ATTR, r);
             * 将其put的,所以在这里能够拿得到
             */
            Route route = exchange.getAttribute(ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR);
            log.info("route=============:{}", JSON.toJSONString(route));

            AtomicReference<String> atomicReference = new AtomicReference<>();
            String json = JSON.toJSONString(requestBody);
            atomicReference.set(json);
            BodyInserter inserter = BodyInserters.fromObject(requestBody);
            HttpHeaders headers = new HttpHeaders();
            headers.putAll(exchange.getRequest().getHeaders());
            headers.remove(HttpHeaders.CONTENT_LENGTH);

            MyCachedBodyOutputMessage outputMessage = new MyCachedBodyOutputMessage(exchange, headers);
            return inserter.insert(outputMessage, new BodyInserterContext())
                    .then(Mono.defer(() -> {
                        ServerHttpRequestDecorator decorator = new ServerHttpRequestDecorator(
                                exchange.getRequest()) {

                            public HttpHeaders getHeaders() {
                                long contentLength = headers.getContentLength();
                                HttpHeaders httpHeaders = new HttpHeaders();
                                httpHeaders.putAll(super.getHeaders());
                                if (contentLength > 0) {
                                    httpHeaders.setContentLength(contentLength);
                                } else {
                                    httpHeaders.set(HttpHeaders.TRANSFER_ENCODING, "chunked");
                                }
                                return httpHeaders;
                            }


                            public Flux<DataBuffer> getBody() {
                                return outputMessage.getBody();
                            }
                        };
                        return chain.filter(exchange.mutate().request(decorator).build());
                    }));
        };

    }

    @Override
    public int getOrder() {
        return -3;
    }
}
