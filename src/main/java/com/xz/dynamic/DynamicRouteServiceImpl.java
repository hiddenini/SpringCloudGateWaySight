package com.xz.dynamic;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.xz.dao.GateWayMapper;
import com.xz.entity.GateWayInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;

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
        gateWayInfo.setRedirectUrl(uri.getScheme() + "://" + uri.getHost());
        gateWayInfo.setRequestPath(pattern);
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
            this.publisher.publishEvent(new RefreshRoutesEvent(this));
            GateWayInfo gateWayInfo = new GateWayInfo();
            URI uri = definition.getUri();
            gateWayInfo.setRedirectUrl(uri.getScheme() + "://" + uri.getHost());
            List<PredicateDefinition> predicates = definition.getPredicates();
            String pattern = definition.getPredicates().get(0).getArgs().get("pattern");
            gateWayInfo.setRequestPath(pattern);
            QueryWrapper<GateWayInfo> queryWrapper = new QueryWrapper<GateWayInfo>().eq("route_name", definition.getId());

            gateWayMapper.update(gateWayInfo, queryWrapper);
            return "success";
        } catch (Exception e) {
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
}
