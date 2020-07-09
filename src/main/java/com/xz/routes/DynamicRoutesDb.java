package com.xz.routes;

import com.xz.dao.GateWayMapper;
import com.xz.entity.GateWayInfo;
import com.xz.filters.EncryptRequestBodyGatewayFilterFactory;
import com.xz.filters.EncryptResponseGatewayFilterFactory;
import com.xz.filters.HeaderGatewayFilterFactory;
import com.xz.filters.ParamsGatewayFilterFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Autowired
    private EncryptRequestBodyGatewayFilterFactory encryptRequestBodyGatewayFilterFactory;

    @Autowired
    private EncryptResponseGatewayFilterFactory encryptResponseGatewayFilterFactory;

    /**
     * 为每一种请求路径配置一个filter(也可以有多个)   难道只能转发到域名上面去?或者服务治理的前缀
     * <p>
     * r.readBody(Map.class, requestBody -> {
     * log.info("requestBody is {}", requestBody);
     * return true;
     * }
     * <p>
     * 是使用了ReadBodyPredicateFactory  这个类调用了ServerWebExchangeUtils.cacheRequestBodyAndRequest
     * <p>
     * 跟到exchange.getAttributes().put(CACHED_REQUEST_BODY_ATTR,
     * dataBuffer);
     * <p>
     * CACHED_REQUEST_BODY_ATTR="cachedRequestBody";
     * <p>
     * Caches the request body in a ServerWebExchange attribute. The attribute is
     *
     * @link CACHED_REQUEST_BODY_ATTR
     * <p>
     * <p>
     * 所以在filter中的可以使用exchange.getAttribute("cachedRequestBodyObject")来获取
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
                            .filters(f -> {
                                f.filters(encryptRequestBodyGatewayFilterFactory.apply(new Object()));
                                f.filters(encryptResponseGatewayFilterFactory.apply(new Object()));
                                return f;
                            })
                            .uri(gateWayInfo.getRedirectUrl())
            ).build();
        });
        return routes.build().getRoutes();
    }
}
