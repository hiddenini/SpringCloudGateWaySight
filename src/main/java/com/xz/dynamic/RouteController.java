package com.xz.dynamic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.cloud.gateway.handler.predicate.PredicateDefinition;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/route")
public class RouteController {
    @Autowired
    private DynamicRouteServiceImpl dynamicRouteService;

    @Autowired
    private DynamicRouteServiceImplDb dynamicRouteServiceDb;

    //增加路由
    @PostMapping("/add")
    public String add(@RequestBody GatewayRouteDefinition gwdefinition) {
        try {
            RouteDefinition definition = assembleRouteDefinition(gwdefinition);
            //return this.dynamicRouteService.add(definition);
            return dynamicRouteServiceDb.add(definition);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "succss";
    }

    //删除路由
    @DeleteMapping("/delete")
    public Mono<ResponseEntity<Object>> delete(@RequestParam String id) {
        //return this.dynamicRouteService.delete(id);
        return this.dynamicRouteServiceDb.delete(id);
    }

    //更新路由
    @PostMapping("/update")
    public String update(@RequestBody GatewayRouteDefinition gwdefinition) {
        RouteDefinition definition = assembleRouteDefinition(gwdefinition);
        //return this.dynamicRouteService.update(definition);
        return this.dynamicRouteServiceDb.update(definition);
    }

    private RouteDefinition assembleRouteDefinition(GatewayRouteDefinition gwdefinition) {

        RouteDefinition definition = new RouteDefinition();

        // ID
        definition.setId(gwdefinition.getId());

        // Predicates
        List<PredicateDefinition> pdList = new ArrayList<>();
        for (GatewayPredicateDefinition gpDefinition : gwdefinition.getPredicates()) {
            PredicateDefinition predicate = new PredicateDefinition();
            predicate.setArgs(gpDefinition.getArgs());
            predicate.setName(gpDefinition.getName());
            pdList.add(predicate);
        }
        definition.setPredicates(pdList);

        // Filters
        List<FilterDefinition> fdList = new ArrayList<>();
        for (GatewayFilterDefinition gfDefinition : gwdefinition.getFilters()) {
            FilterDefinition filter = new FilterDefinition();
            filter.setArgs(gfDefinition.getArgs());
            filter.setName(gfDefinition.getName());
            fdList.add(filter);
        }
        definition.setFilters(fdList);

        // URI
        URI uri = UriComponentsBuilder.fromUriString(gwdefinition.getUri()).build().toUri();
        definition.setUri(uri);

        return definition;
    }
}
