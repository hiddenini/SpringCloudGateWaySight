package com.xz.routes;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author xz
 * @date 2020/6/5 15:06
 **/

@Configuration
public class GatewayRoutes {

    /**
     * 根据路径做转发
     * <p>
     * patters 指的是 符合pattern表达式的请求将会进行转发
     * <p>
     * stripPrefix(int num) 指的是会剥离掉num数量个前缀 将剩下的拼接到uri后面
     * <p>
     * eg  规则如下
     * r.path("/kid/**")
     * .filters(f -> f.stripPrefix(1))
     * .uri("http://localhost:8085")
     * 访问http://localhost:8080/kid/api/hello 会被转发到http://localhost:8085/api/hello
     * 访问http://localhost:8080/kid/api/second 会被转发到http://localhost:8085/api/second
     * <p>
     * <p>
     * eg  规则如下
     * r.path("/kid/crm/**")
     * .filters(f -> f.stripPrefix(2))
     * .uri("http://localhost:8085")
     * 访问http://localhost:8080/kid/crm/api/hello 会被转发到http://localhost:8085/api/hello
     * 访问http://localhost:8080/kid/crm/api/second 会被转发到http://localhost:8085/api/second
     */
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                .route(r ->
                        r.path("/kid/crm/**")
                                .filters(f -> f.stripPrefix(2))
                                .uri("http://localhost:8085")
                )
                .build();
    }
}
