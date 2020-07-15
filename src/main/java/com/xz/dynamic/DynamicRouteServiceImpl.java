package com.xz.dynamic;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xz.dao.GateWayMapper;
import com.xz.entity.GateWayInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.*;

@Slf4j
@Service
public class DynamicRouteServiceImpl implements ApplicationEventPublisherAware {
    @Autowired
    private RouteDefinitionWriter routeDefinitionWriter;

    private ApplicationEventPublisher publisher;

    @Autowired
    private GateWayMapper gateWayMapper;

    //增加路由
    public String add(RouteDefinition definition) {
        routeDefinitionWriter.save(Mono.just(definition)).subscribe();
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
        List<PredicateDefinition> predicates = definition.getPredicates();
        String pattern = definition.getPredicates().get(0).getArgs().get("pattern");
        GateWayInfo gateWayInfo = new GateWayInfo();
        URI uri = definition.getUri();
        gateWayInfo.setRouteName(definition.getId());
        //gateWayInfo.setRedirectUrl(uri.getScheme() + "://" + uri.getHost());
        gateWayInfo.setRedirectUrl(uri.toString());
        gateWayInfo.setRequestPath(pattern);
        loadRouteToMemory(gateWayInfo);
        gateWayMapper.insert(gateWayInfo);
        return "success";
    }

    //更新路由
    public String update(RouteDefinition definition) {
        try {
            this.routeDefinitionWriter.delete(Mono.just(definition.getId()));
        } catch (Exception e) {
            return "update fail,not find route routeId: " + definition.getId();
        }
        try {
            routeDefinitionWriter.save(Mono.just(definition)).subscribe();
            GateWayInfo gateWayInfo = new GateWayInfo();
            URI uri = definition.getUri();
            gateWayInfo.setRouteName(definition.getId());
            gateWayInfo.setRedirectUrl(uri.toString());
            List<PredicateDefinition> predicates = definition.getPredicates();
            String pattern = definition.getPredicates().get(0).getArgs().get("pattern");
            gateWayInfo.setRequestPath(pattern);
            QueryWrapper<GateWayInfo> queryWrapper = new QueryWrapper<GateWayInfo>().eq("route_name", definition.getId());
            loadRouteToMemory(gateWayInfo);
            gateWayMapper.update(gateWayInfo, queryWrapper);
            loadRouteToMemory(gateWayInfo);
            this.publisher.publishEvent(new RefreshRoutesEvent(this));
            return "success";
        } catch (Exception e) {
            e.printStackTrace();
            return "update route fail";
        }
    }

    //删除路由
    public Mono<ResponseEntity<Object>> delete(String id) {
        QueryWrapper<GateWayInfo> queryWrapper = new QueryWrapper<GateWayInfo>().eq("route_name", id);
        gateWayMapper.delete(queryWrapper);
        this.routeDefinitionWriter.delete(Mono.just(id)).subscribe();
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
/*        return this.routeDefinitionWriter.delete(Mono.just(id))
                .then(Mono.defer(() -> Mono.just(ResponseEntity.ok().build())))
                .onErrorResume(t -> t instanceof NotFoundException, t -> Mono.just(ResponseEntity.notFound().build()));*/

        return null;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }

    /**
     * 调用add或者update时也刷到内存
     */
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

}
