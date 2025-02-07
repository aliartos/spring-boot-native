package com.example.simplehelloworld.hello_world.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class HelloWorldController {
    @GetMapping("/hello")
    public Mono<String> sayHello() {
        return Mono.just("Hello, World!");
    }
}
