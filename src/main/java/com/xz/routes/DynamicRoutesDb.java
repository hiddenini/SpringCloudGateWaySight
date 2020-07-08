package com.xz.routes;

import com.xz.dao.GateWayMapper;
import com.xz.entity.GateWayInfo;
import com.xz.filters.HeaderGatewayFilterFactory;
import com.xz.filters.ParamsGatewayFilterFactory;
import com.xz.filters.PostFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.factory.AbstractNameValueGatewayFilterFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@Slf4j
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

/*    @Autowired
    private PostFilter postFilter;*/

    /**
     * 为每一种请求路径配置一个filter(也可以有多个)   难道只能转发到域名上面去?或者服务治理的前缀
     */
    @Override
    public Flux<Route> getRoutes() {
        RouteLocatorBuilder.Builder routes = builder.routes();
        List<GateWayInfo> gateWayInfos = gateWayMapper.selectList(null);
        gateWayInfos.stream().forEach(gateWayInfo -> {
            routes.route(r ->
                            r.readBody(Map.class, requestBody -> {
                                log.info("requestBody is {}", requestBody);
                                return true;
                            }).and().path(gateWayInfo.getRequestPath())
                                    //.filters(f -> f.filter(postFilter.apply(new AbstractNameValueGatewayFilterFactory.NameValueConfig())))
                                    //.filters(f -> f.stripPrefix(1))
//                            .filters(f -> f.filter(headerGatewayFilterFactory.apply(new Object())))
                                    //.filters(f -> f.filter(unionResultGatewayFilterFactory.apply(new ModifyResponseBodyGatewayFilterFactory.Config())))
                                    .uri(gateWayInfo.getRedirectUrl())
            ).build();
        });
        return routes.build().getRoutes();
    }
}
