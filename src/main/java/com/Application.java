package com;

import com.xz.util.SpringUtil;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author xz
 * @date 2020/6/5 15:03
 **/
//@EnableScheduling
@SpringBootApplication()
@MapperScan("com.xz.dao*")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
        /**
         * 这里拿到是CachingRouteLocator delegate delegates iterable size=2 DynamicRoutesDb 和RouteDefinitionRouteLocator
         */
        RouteLocator bean = SpringUtil.getBean(RouteLocator.class);
        System.out.println("routeLocator"+SpringUtil.getBean(RouteLocator.class));
    }

}
