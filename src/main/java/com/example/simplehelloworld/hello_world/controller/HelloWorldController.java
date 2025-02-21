package com.example.simplehelloworld.hello_world.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class HelloWorldController {
    @GetMapping("/")
    public Mono<String> sayHello1() {
        return Mono.just("Hello, World! 222");
    }

    @GetMapping("/hello")
    public Mono<String> sayHello2() {
        return Mono.just("Hello, World!");
    }
}
