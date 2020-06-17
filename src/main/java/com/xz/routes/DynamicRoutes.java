package com.xz.routes;

import com.xz.filters.HeaderGatewayFilterFactory;
import com.xz.filters.ParamsGatewayFilterFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import reactor.core.publisher.Flux;

//@Configuration
public class DynamicRoutes implements RouteLocator {

    @Autowired
    RouteLocatorBuilder builder;

    @Autowired
    private HeaderGatewayFilterFactory headerGatewayFilterFactory;

    @Autowired
    private ParamsGatewayFilterFactory paramsGatewayFilterFactory;


    /**
     * 为每一种请求路径配置一个filter(也可以有多个)
     */
    @Override
    public Flux<Route> getRoutes() {
        RouteLocatorBuilder.Builder routes = builder.routes();
        routes.route(r ->
                r.path("/hello/gateWay/header/**")
                        .filters(f -> f.filter(headerGatewayFilterFactory.apply(new Object())))
                        .uri("http://localhost:8087")
        ).build();

        routes.route(r ->
                r.path("/hello/gateWay/params/**")
                        .filters(f -> f.filter(paramsGatewayFilterFactory.apply(new Object())))
                        .uri("http://localhost:8087")
        ).build();
        return routes.build().getRoutes();
    }
}
