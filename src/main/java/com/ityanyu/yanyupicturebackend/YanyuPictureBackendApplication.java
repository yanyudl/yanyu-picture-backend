package com.ityanyu.yanyupicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableAsync
@MapperScan("com.ityanyu.yanyupicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class YanyuPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(YanyuPictureBackendApplication.class, args);
    }

}
