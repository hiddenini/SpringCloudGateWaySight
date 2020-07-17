package com.xz.routes;

import com.xz.dao.GateWayMapper;
import com.xz.dynamic.DynamicRouteServiceImplDb;
import com.xz.entity.GateWayInfo;
import com.xz.filters.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 实现动态路由，有两种方式
 * <p>
 * 方式1--使用GatewayAutoConfiguration中默认配置的RouteLocator  类型是CachingRouteLocator，它包装了CompositeRouteLocator
 * <p>
 * 而CompositeRouteLocator则组合了RouteDefinitionRouteLocator. RouteDefinitionRouteLocator中注入了 RouteDefinitionLocator routeDefinitionLocator
 * <p>
 * 类型是CompositeRouteDefinitionLocator，它组合了InMemoryRouteDefinitionRepository、PropertiesRouteDefinitionLocator
 * <p>
 * RouteDefinitionRouteLocator的getRoutes方法，从routeDefinitionLocator.getRouteDefinitions()(RouteDefinitionLocator有多种实现,默认是默认的实现是InMemoryRouteDefinitionRepository)，然后使用convertToRoute方法进行转换，该方法主要用到了combinePredicates、getFilters两个方法
 * <p>
 * 注意:RouteDefinitionRouteLocator与RouteDefinitionLocator比较容易混淆，前者是一个RouteLocator，后者是一个RouteDefinitionLocator，前者的RouteDefinitionRouteLocator主要从后者获取路由定义信息。
 * <p>
 * 按照上面的描述,可以注入RouteDefinitionWriter在controller层进行add或者update或者delete时进行save or delete,默认的实现是InMemoryRouteDefinitionRepository,TODO 这里可以自定义自己的实现
 * <p>
 * 接下来是要处理初始化的问题,eg:{@link ApplicationStartUp}  初始化时也使用routeDefinitionWriter将路由信息存起来,对于InMemoryRouteDefinitionRepository而言,是保存在了内存中
 * <p>
 * 这样的话,就完成了所有的流程,既可以在程序启动时从数据库加在路由信息并且为其设置predicate和filters  并且程序启动之后通过接口操作的路由也保存到了数据库并且刷到了内存。
 * <p>
 * <p>
 * note:
 * Gateway提供的Actuator接口  可以对路由信息进行操作,在AbstractGatewayControllerEndpoint中save 和delete方法使用的是routeDefinitionWriter 的save和delete方法
 * <p>
 * 而RouteDefinitionWriter现在唯一的实现是InMemoryRouteDefinitionRepository也就是通过gateway提供的Actuator 例如actuator/gateway/routes  	@PostMapping("/routes/{id}")
 *
 * @DeleteMapping("/routes/{id}") 是可以对路由信息进行操作的。但是这里我们是自己提供的controller,在自定义的接口中将路由信息保存到了数据库，但是我又想通过Actuator接口访问到这些路由,所以在接口中使用了
 * <p>
 * RouteDefinitionWriter进行操作,但是如果使用下面的第二种方式,并且在接口层只写数据库，并没有使用RouteDefinitionWriter
 * <p>
 * <p>
 * 在GatewayAutoConfiguration注入了一个WeightCalculatorWebFilter  其onApplicationEvent方法中 else if (event instanceof RefreshRoutesEvent && routeLocator != null)
 * <p>
 * 判断了如果当前的事件是RefreshRoutesEvent并且routeLocator不为空,则进行	routeLocator.ifAvailable(locator -> locator.getRoutes().subscribe());
 * <p>
 * 调用当前的locator的locator.getRoutes() 所以只要this.publisher.publishEvent(new RefreshRoutesEvent(this)); publishEvent了一个RefreshRoutesEvent,那么就会调用当前容器内的
 * <p>
 * RouteLocator的getRoutes()方法进行路由信息的刷新  gateway在接收请求是是利用RoutePredicateHandlerMapping用于路由的查找,在其lookupRoute方法中调用的是this.routeLocator.getRoutes()
 * <p>
 * 而RoutePredicateHandlerMapping是在GatewayAutoConfiguration进行配置的,也就是说此时的this.routeLocator就是容器中的RouteLocator
 * <p>
 * 所以方法1是使用的默认的RouteLocator 其getRoutes()实现其实是RouteDefinitionRouteLocator的getRoutes(),最终实现是this.routeDefinitionLocator.getRouteDefinitions()
 * <p>
 * this.routeDefinitionLocator是InMemoryRouteDefinitionRepository的getRouteDefinitions()
 * <p>
 * 方法2是使用的自定义的RouteLocator,并实现了其getRoutes()方法
 * <p>
 * 那么问题来了,DynamicRoutesDb实现了RouteLocator 并使用@Component标注了,那么容器中的RouteLocator就是DynamicRoutesDb吗?
 * <p>
 * 容器中的RouteLocator类型是CachingRouteLocator delegate delegates iterable size=2 DynamicRoutesDb 和RouteDefinitionRouteLocator
 * <p>
 * <p>
 * 方式2--使用自定义的RouteLocator eg:{@link DynamicRoutesDb}
 * <p>
 * 自定义RouteLocator将其注入到spring容器,在GatewayAutoConfiguration中注入了一个RouteDefinitionRouteLocator 加上我们自定义的DynamicRoutesDb
 * <p>
 * 容器中是有两个RouteLocator的，通过CompositeRouteLocator包装，再通过CachingRouteLocator包装后。将2个RouteLocator包含在一起,
 * <p>
 * 在访问/actuator/gateway/routes时既执行了DynamicRoutesDb.getRoutes() 也执行了RouteDefinitionRouteLocator.getRoutes() 但是后续比如add时如何调用的这2个
 * <p>
 * RouteLocator暂时还不清楚 add的时候 this.publisher.publishEvent(new RefreshRoutesEvent(this));发布了一个 RefreshRoutesEvent事件，相关的监听类是
 * <p>
 * CachingRouteDefinitionLocator 以及WeightCalculatorWebFilter
 * <p>
 * TODO 这2个RouteLocator到底是如何被调用的以及add时为什么没有打印DynamicRoutesDb中的  log.info("enter DynamicRoutesDb")?
 * <p>
 * <p>
 * 现阶段不太了解reactor,debug不出来.暂时放一下。总结来说,2种方式都是可以的,但是自定义RouteLocator相对方便一些,可以结合
 * <p>
 * {@link DynamicRouteServiceImplDb} 来完成动态路由
 * <p>
 * 方式一相比会麻烦一些,并且使用默认的InMemoryRouteDefinitionRepository在集群环境下可能会有问题,需要自定义.并且方式一设置predicate和filters时比较麻烦
 * <p>
 */


@Slf4j
@Component
public class DynamicRoutesDb implements RouteLocator, ApplicationEventPublisherAware {

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

    private ApplicationEventPublisher publisher;

    @Autowired
    private RouteDefinitionWriter routeDefinitionWriter;

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
     * <p>
     * 如果想要路由一个不带参数的接口,并且这里设置了r.readBody(Map.class, requestBody -> {}
     * <p>
     * 那么会一直404,因为找不到对应的路由，虽然路径匹配但是predicate不通过,
     * <p>
     * 自己测试的话可以随便给一个参数，一般来说路由的接口都是有参数的
     * <p>
     * Spring-Cloud-Gateway之请求处理流程
     * <p>
     * DispatcherHandler：所有请求的调度器，负载请求分发
     * RoutePredicateHandlerMapping:路由谓语匹配器，用于路由的查找，以及找到路由后返回对应的WebHandler，DispatcherHandler会依次遍历HandlerMapping集合进行处理
     * FilteringWebHandler : 使用Filter链表处理请求的WebHandler，RoutePredicateHandlerMapping找到路由后返回对应的FilteringWebHandler对请求进行处理，FilteringWebHandler负责组装Filter链表并调用链表处理请求。
     * <p>
     * 转发路由时默认使用的是netty  先在NettyRoutingFilter将CLIENT_RESPONSE_CONN_ATTR put exchange.getAttributes().put(CLIENT_RESPONSE_CONN_ATTR, connection);
     * <p>
     * 然后在NettyWriteResponseFilter中     	Connection connection = exchange.getAttribute(CLIENT_RESPONSE_CONN_ATTR);
     * <p>
     * 拿到对应的connection进行连接  使用的是reactor-netty的东西
     */
    @Override
    public Flux<Route> getRoutes() {
        log.info("enter DynamicRoutesDb");
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

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
        this.publisher = applicationEventPublisher;
    }

    /**
     * 定时刷新路由
     * TODO 添加了新的接口后，以Event驱动刷新路由
     */
/*    @Scheduled(initialDelay = 1000L, fixedDelay = 10000L)
    public void refresh() {
        log.info("refresh=============");
        this.publisher.publishEvent(new RefreshRoutesEvent(this));
    }*/


}
