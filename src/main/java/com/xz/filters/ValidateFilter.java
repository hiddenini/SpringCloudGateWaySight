package com.xz.filters;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xz.rewrite.MyCachedBodyOutputMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.support.BodyInserterContext;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserter;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 这种也是ok的
 */

@Slf4j
@Component
public class ValidateFilter implements GlobalFilter, Ordered {

    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        /**
         * gateWay2.1.0版本中有这个DefaultServerRequest,换成高版本没有了
         */
        //ServerRequest serverRequest = new DefaultServerRequest(exchange);
        /**
         * ReadBodyPredicateFactory ，发现里面缓存了request body的信息，于是在自定义router中配置了ReadBodyPredicateFactory
         *
         * 然后在filter中通过cachedRequestBodyObject缓存字段获取request body信息，这种解决，
         *
         * 一不会带来重复读取问题
         *
         * 二不会带来requestBody取不全问题。
         *
         * 三在低版本的Spring Cloud Finchley.SR2也可以运行
         */
        Map<String, Object> cachedRequestBodyObject = exchange.getAttribute("cachedRequestBodyObject");
        log.info("cachedRequestBodyObject:{}", cachedRequestBodyObject);
        ObjectMapper mapper = new ObjectMapper();
        AtomicReference<String> atomicReference = new AtomicReference<>();
        try {
            String json = mapper.writeValueAsString(cachedRequestBodyObject);
            atomicReference.set(json);
            log.info("json:{}", json);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
/*        cachedRequestBodyObject.forEach((String s, Object obj) -> {
            log.info("key:{},value:{}", s, obj);
        });*/
        // mediaType
        MediaType mediaType = exchange.getRequest().getHeaders().getContentType();
        // read & modify body 直接将serverRequest中的body包装成新的request 跟着上面的DefaultServerRequest版本走
/*        Mono<String> modifiedBody = serverRequest.bodyToMono(String.class)
                .flatMap(body -> {
                    log.info("ValidateFilter body:{}", body);
                    return Mono.just(body);
                });*/

        /**
         * 将cachedRequestBodyObject作为requestBody传递包装成新的request 跟着上面的DefaultServerRequest版本走
         */
/*        Mono<String> modifiedBody = serverRequest.bodyToMono(String.class)
                .flatMap(body -> {
                    log.info("ValidateFilter cachedRequestBodyObject:{}", body);
                    return Mono.just(atomicReference.get());
                });*/

        BodyInserter bodyInserter = BodyInserters.fromObject(cachedRequestBodyObject);
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(exchange.getRequest().getHeaders());
        // the new content type will be computed by bodyInserter
        // and then set in the request decorator
        headers.remove(HttpHeaders.CONTENT_LENGTH);

        MyCachedBodyOutputMessage outputMessage = new MyCachedBodyOutputMessage(exchange, headers);
        return bodyInserter.insert(outputMessage, new BodyInserterContext())
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
    }


    public int getOrder() {
        return -3;
    }

}
