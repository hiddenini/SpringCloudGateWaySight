package com.xz.routes;

import com.xz.dao.GateWayMapper;
import com.xz.entity.GateWayInfo;
import com.xz.filters.HeaderGatewayFilterFactory;
import com.xz.filters.ParamsGatewayFilterFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.List;

@Configuration
public class DynamicRoutesDb implements RouteLocator {

    @Autowired
    RouteLocatorBuilder builder;

    @Autowired
    private HeaderGatewayFilterFactory headerGatewayFilterFactory;

    @Autowired
    private ParamsGatewayFilterFactory paramsGatewayFilterFactory;

    @Autowired
    private GateWayMapper gateWayMapper;

    /**
     * 为每一种请求路径配置一个filter(也可以有多个)
     */
    @Override
    public Flux<Route> getRoutes() {
        RouteLocatorBuilder.Builder routes = builder.routes();
        List<GateWayInfo> gateWayInfos = gateWayMapper.selectList(null);
        gateWayInfos.stream().forEach(gateWayInfo -> {
            routes.route(r ->
                            r.path(gateWayInfo.getRequestPath())
//                            .filters(f -> f.filter(headerGatewayFilterFactory.apply(new Object())))
                                    .uri(gateWayInfo.getRedirectUrl())
            ).build();
        });
        return routes.build().getRoutes();
    }
}
