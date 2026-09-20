package com.ckrey.autobackworkflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.ckrey.autobackworkflow.mapper")
public class AutoBackWorkFlowApplication {

    public static void main(String[] args) {
        SpringApplication.run(AutoBackWorkFlowApplication.class, args);
    }

}
