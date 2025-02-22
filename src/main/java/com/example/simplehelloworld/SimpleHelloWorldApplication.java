package com.example.simplehelloworld;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
// com.example.zip, com.example.simplehelloworld
@ComponentScan(basePackages = {"com.example.zip", "com.example.simplehelloworld"})
public class SimpleHelloWorldApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleHelloWorldApplication.class, args);
    }

}
