package com.xz.routes;

import com.xz.dao.GateWayMapper;
import com.xz.entity.GateWayInfo;
import com.xz.filters.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

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

    @Autowired
    private CheckSignatureGatewayFilterFactory checkSignatureGatewayFilterFactory;

    @Autowired
    private CheckRequiredHeadersGatewayFilterFactory checkRequiredHeadersGatewayFilterFactory;

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
     *
     * 如果想要路由一个不带参数的接口,并且这里设置了r.readBody(Map.class, requestBody -> {}
     *
     * 那么会一直404,因为找不到对应的路由，虽然路径匹配但是predicate不通过,
     *
     * 自己测试的话可以随便给一个参数，一般来说路由的接口都是有参数的
     *
     * Spring-Cloud-Gateway之请求处理流程
     *
     * DispatcherHandler：所有请求的调度器，负载请求分发
     * RoutePredicateHandlerMapping:路由谓语匹配器，用于路由的查找，以及找到路由后返回对应的WebHandler，DispatcherHandler会依次遍历HandlerMapping集合进行处理
     * FilteringWebHandler : 使用Filter链表处理请求的WebHandler，RoutePredicateHandlerMapping找到路由后返回对应的FilteringWebHandler对请求进行处理，FilteringWebHandler负责组装Filter链表并调用链表处理请求。
     *
     */
    @Override
    public Flux<Route> getRoutes() {
        RouteLocatorBuilder.Builder routes = builder.routes();
        List<GateWayInfo> gateWayInfos = gateWayMapper.selectList(null);
        gateWayInfos.stream().forEach(gateWayInfo -> {
            AtomicReference<URI> atomicReference = new AtomicReference<>();
            try {
                URI uri = new URI(gateWayInfo.getRedirectUrl());
                atomicReference.set(uri);
            } catch (URISyntaxException e) {
                e.printStackTrace();
            }
            routes.route(gateWayInfo.getRouteName(), r ->
                    r.readBody(Map.class, requestBody -> {
                        log.info("requestBody is {}", requestBody);
                        return true;
                    }).and().path(gateWayInfo.getRequestPath())
                            .filters(f -> {
                                f.filters(checkRequiredHeadersGatewayFilterFactory.apply(new Object()));
                                f.filters(checkSignatureGatewayFilterFactory.apply(new Object()));
                                f.filters(encryptRequestBodyGatewayFilterFactory.apply(new Object()));
                                f.filters(encryptResponseGatewayFilterFactory.apply(new Object()));
                                return f;
                            })
                            .uri(atomicReference.get())
            ).build();
        });
        return routes.build().getRoutes();
    }
}
