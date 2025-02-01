package com.boilerplate.spring_boot.healthcheck;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HealthCheckController {

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}
