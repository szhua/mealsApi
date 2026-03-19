package com.sancanji.mealsapi;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.sancanji.mealsapi.mapper")
public class MealsapiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MealsapiApplication.class, args);
    }
}