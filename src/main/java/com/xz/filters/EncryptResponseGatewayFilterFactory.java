package com.xz.filters;

import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
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

@Slf4j
@Component
public class EncryptResponseGatewayFilterFactory extends AbstractGatewayFilterFactory<Object> {
    @Override
    public GatewayFilter apply(Object config) {
        return new InnerFilter(config);
    }

    /**
     * 注意我们通过这种工厂创建出来的过滤器是没有指定order的，会被默认设置为是0，配置在yml文件中，则按照它书写的顺序来执行
     * <p>
     * 如果想要在代码中设置好它的顺序，工厂的apply方法需要做一些修改
     * <p>
     * 创建一个内部类，来实现2个接口，指定顺序
     */
    private class InnerFilter implements GatewayFilter, Ordered {

        private Object config;

        InnerFilter(Object config) {
            this.config = config;
        }

        @Override
        public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
            log.info("enter EncryptResponseGatewayFilterFactory");
            ServerHttpResponse serverHttpResponse = exchange.getResponse();
            DataBufferFactory bufferFactory = serverHttpResponse.bufferFactory();
            ServerHttpResponseDecorator response = new ServerHttpResponseDecorator(serverHttpResponse) {

                @Override
                public Mono<Void> writeWith(Publisher<? extends DataBuffer> body) {
                    //返回值添加request-id(后面可以做成trace-id)
                    serverHttpResponse.getHeaders().set("request-id", String.valueOf(UUID.randomUUID()));
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
                            log.info("EncryptResponseGatewayFilterFactory路由返回的response:{}", str);
                            serverHttpResponse.getHeaders().setContentLength(str.getBytes().length);
                            return bufferFactory.wrap(str.getBytes());

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

        /**
         * NettyWriteResponseFilter  的order是-1,这里的order要比-1小才行 如果是0则不生效了
         * @return
         */
        @Override
        public int getOrder() {
            return -2;
        }
    }


}
