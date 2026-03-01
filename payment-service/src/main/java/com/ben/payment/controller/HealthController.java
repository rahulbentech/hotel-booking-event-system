package com.ben.payment.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@SuppressWarnings("ALL")
@RestController
public class HealthController {

    @GetMapping("/actuator/health/readiness")
    public Mono<String> readiness() {
        return Mono.just("ready");
    }

    @GetMapping("/actuator/health/liveness")
    public Mono<String> liveness() {
        return Mono.just("alive");
    }
}