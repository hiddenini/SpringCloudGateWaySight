package com.xz.routes;

import com.xz.config.CustomPredicate;
import com.xz.dao.GateWayMapper;
import com.xz.entity.GateWayInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 在程序初始化时读取数据库的gateWayInfo  包装成RouteDefinition   使用默认的InMemoryRouteDefinitionRepository将其刷到内存中
 * <p>
 * 设置filter, filterDefinition.setName("CheckRequiredHeaders"); 这里的名字是自定义的gatewayFilterFactory(eg CheckRequiredHeadersGatewayFilterFactory
 * <p>
 * CheckSignatureGatewayFilterFactory
 * <p>
 * )需要去掉"GatewayFilterFactory"  原因是在GatewayAutoConfiguration中配置routeDefinitionRouteLocator时-->factory -> this.gatewayFilterFactories.put(factory.name(), factory));
 * <p>
 * factory.name() --> NameUtils.normalizeFilterFactoryName(getClass()); 将GatewayFilterFactory从类名中去掉了
 * <p>
 * 后面在RouteDefinitionRouteLocator   -->   loadGatewayFilters中   -->GatewayFilterFactory factory = this.gatewayFilterFactories
 * .get(definition.getName()); 这里是根据名字去拿具体的GatewayFilterFactory
 * <p>
 * 设置PredicateDefinition
 * 注意设置readBody时需要自定义predicate 并且需要有占位符
 *
 * todo 这样做了之后初始化的所有路由都要配置上了filter和predicate 但是add时没有，也需要手动添加
 *
 *
 */

@Slf4j
//@Component
public class ApplicationStartUp implements ApplicationRunner, ApplicationEventPublisherAware {
    @Autowired
    private GateWayMapper gateWayMapper;

    private ApplicationEventPublisher publisher;

    @Autowired
    private RouteDefinitionWriter routeDefinitionWriter;

    @Override
    public void run(ApplicationArguments args) {
        log.info("enter ApplicationStartUp");
        List<GateWayInfo> gateWayInfos = gateWayMapper.selectList(null);
        for (GateWayInfo gateWayInfo : gateWayInfos) {
            this.loadRouteToMemory(gateWayInfo);
        }
    }

    public void loadRouteToMemory(GateWayInfo gateWayInfo) {
        RouteDefinition routeDefinition = new RouteDefinition();
        routeDefinition.setId(gateWayInfo.getRouteName());
        URI uri = UriComponentsBuilder.fromUriString(gateWayInfo.getRedirectUrl()).build().toUri();
        routeDefinition.setUri(uri);
        /**
         * 设置FilterDefinition
         */
        List<FilterDefinition> filters = new ArrayList<>();
        FilterDefinition filterDefinition = new FilterDefinition();
        filterDefinition.setName("CheckRequiredHeaders");

        FilterDefinition filterDefinition1 = new FilterDefinition();
        filterDefinition1.setName("CheckSignature");

        filters.add(filterDefinition);
        filters.add(filterDefinition1);

        routeDefinition.setFilters(filters);

        /**
         * 设置PredicateDefinition
         */
        List<PredicateDefinition> predicates = new ArrayList<>();
        PredicateDefinition predicateDefinition = new PredicateDefinition();
        PredicateDefinition predicateDefinition1 = new PredicateDefinition();
        Map<String, String> args = new HashMap<>();
        args.put("pattern", gateWayInfo.getRequestPath());
        predicateDefinition.setArgs(args);
        predicateDefinition.setName("Path");
        predicateDefinition1.setName("ReadBodyPredicateFactory");
        Map<String, String> args1 = new HashMap<>();
        args1.put("inClass", "#{T(Object)}");
        //args1.put("inClass", Object.class.getName());
        /**
         *自定义predicate时需要占位符,跟了半天,主要是在RouteDefinitionRouteLocator-->combinePredicates-->lookup-->  factory.shortcutType().normalize(args, factory,
         * 	this.parser, this.beanFactory); ConfigurationUtils.bind-->。。。
         *
         * -->Binder.bindProperty
         */
        args1.put("predicate", "#{@customPredicate}");
        //args1.put("predicate", CustomPredicate.class.getName());
        predicateDefinition1.setArgs(args1);
        predicates.add(predicateDefinition);
        predicates.add(predicateDefinition1);
        routeDefinition.setPredicates(predicates);
        routeDefinitionWriter.save(Mono.just(routeDefinition)).subscribe();
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }
}
