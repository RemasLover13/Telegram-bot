package com.remaslover.telegrambotaq.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "Telegram Bot");
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }

    @GetMapping("/")
    public String home() {
        return """
            <html>
            <body>
                <h1>🤖 Telegram Bot</h1>
                <p>Status: <strong>RUNNING</strong></p>
                <p>Time: %s</p>
                <p>API: <a href="/ping">/ping</a> for health check</p>
            </body>
            </html>
            """.formatted(LocalDateTime.now());
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("version", "1.0");
        health.put("keepAlive", "enabled");
        return health;
    }
}
