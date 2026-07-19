package com.maplemetric;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MaplemetricServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaplemetricServiceApplication.class, args);
    }

}
