package com.dianping;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@MapperScan("com.dianping.mapper")
@EnableTransactionManagement
public class DianpingApplication {
    public static void main(String[] args) {
        SpringApplication.run(DianpingApplication.class, args);
    }
}
