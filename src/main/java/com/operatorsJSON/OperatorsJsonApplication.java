package com.operatorsJSON;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableCaching
public class OperatorsJsonApplication {

    public static void main(String[] args) {
        ApplicationContext ctx = SpringApplication.run(OperatorsJsonApplication.class, args);
        System.out.println("Server started!");
    }

}
