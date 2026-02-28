package com.k44.stn.common.web.api;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/test")
    public Map<String, String> test() {
        return Map.of("status", "ok");
    }
}