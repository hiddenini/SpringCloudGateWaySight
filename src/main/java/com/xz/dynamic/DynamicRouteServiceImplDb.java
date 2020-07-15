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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 配合方式2自定义RouteLocator使用
 */
@Slf4j
@Service
public class DynamicRouteServiceImplDb implements ApplicationEventPublisherAware {

    private ApplicationEventPublisher publisher;

    @Autowired
    private GateWayMapper gateWayMapper;

    //增加路由
    public String add(RouteDefinition definition) {
        List<PredicateDefinition> predicates = definition.getPredicates();
        String pattern = definition.getPredicates().get(0).getArgs().get("pattern");
        GateWayInfo gateWayInfo = new GateWayInfo();
        URI uri = definition.getUri();
        gateWayInfo.setRouteName(definition.getId());
        gateWayInfo.setRedirectUrl(uri.toString());
        gateWayInfo.setRequestPath(pattern);
        gateWayMapper.insert(gateWayInfo);
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
        return "success";
    }

    //更新路由
    public String update(RouteDefinition definition) {
        GateWayInfo gateWayInfo = new GateWayInfo();
        URI uri = definition.getUri();
        gateWayInfo.setRouteName(definition.getId());
        gateWayInfo.setRedirectUrl(uri.toString());
        List<PredicateDefinition> predicates = definition.getPredicates();
        String pattern = definition.getPredicates().get(0).getArgs().get("pattern");
        gateWayInfo.setRequestPath(pattern);
        QueryWrapper<GateWayInfo> queryWrapper = new QueryWrapper<GateWayInfo>().eq("route_name", definition.getId());
        gateWayMapper.update(gateWayInfo, queryWrapper);
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
        return "success";
    }

    //删除路由
    public Mono<ResponseEntity<Object>> delete(String id) {
        QueryWrapper<GateWayInfo> queryWrapper = new QueryWrapper<GateWayInfo>().eq("route_name", id);
        gateWayMapper.delete(queryWrapper);
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
        return null;
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }

}
