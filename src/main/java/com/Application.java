package com;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * @author xz
 * @date 2020/6/5 15:03
 **/
@SpringBootApplication()
@MapperScan("com.xz.dao*")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

}
