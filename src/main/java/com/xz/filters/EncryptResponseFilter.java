package com.xz.filters;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.UUID;

/**
 * 拿到路由返回的结果并在头部塞入request-id 返回给请求方
 * <p>
 * 如果是实现GlobalFilter 那么order可以通过实现Ordered来指定
 */

@Slf4j
//@Component
public class EncryptResponseFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        Object object = exchange.getAttribute("cachedRequestBodyObject");
        ServerHttpResponse originalResponse = exchange.getResponse();
        DataBufferFactory bufferFactory = originalResponse.bufferFactory();
        ServerHttpResponseDecorator response = new ServerHttpResponseDecorator(originalResponse) {

            @Override
            public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                //返回值添加request-id(后面可以做成trace-id)
                originalResponse.getHeaders().set("request-id", String.valueOf(UUID.randomUUID()));
                if (getStatusCode().equals(HttpStatus.OK) && body instanceof Flux) {
                    Flux<? extends DataBuffer> fluxBody = Flux.from(body);
                    return super.writeWith(fluxBody.map(dataBuffer -> {
                        DataBufferFactory dataBufferFactory = new DefaultDataBufferFactory();
                        DataBuffer join = dataBufferFactory.join(Arrays.asList(dataBuffer));
                        byte[] content = new byte[join.readableByteCount()];
                        join.read(content);
                        // 释放掉内存
                        DataBufferUtils.release(join);
                        String str = new String(content, Charset.forName("UTF-8"));
                        log.info("路由返回的response:{}", str);
                        originalResponse.getHeaders().setContentLength(str.getBytes().length);
                        return bufferFactory.wrap(str.getBytes());

                        /**
                         * 数据过长时可能被被截断
                         */
/*                        // probably should reuse buffers
                        byte[] content = new byte[dataBuffer.readableByteCount()];
                        dataBuffer.read(content);
                        //释放掉内存
                        DataBufferUtils.release(dataBuffer);
                        String s = new String(content, Charset.forName("UTF-8"));
                        //System.out.println("s======="+s);
                        //TODO，s就是response的值，想修改、查看就随意而为了
                        byte[] uppedContent = s.getBytes();
                        return bufferFactory.wrap(uppedContent);*/
                    }));
                }
                return super.writeWith(body);
            }

            @Override
            public Mono<Void> writeAndFlushWith(Publisher<? extends Publisher<? extends DataBuffer>> body) {
                return writeWith(Flux.from(body)
                        .flatMapSequential(p -> p));
            }
        };
        return chain.filter(exchange.mutate().response(response).build());
    }

    @Override
    public int getOrder() {
        return -2;
    }
}
