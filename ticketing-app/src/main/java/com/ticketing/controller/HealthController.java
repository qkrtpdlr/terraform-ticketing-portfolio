package com.ticketing.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final DataSource dataSource;
    private final RedisTemplate<String, String> redisTemplate;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now());

        // Database check
        try (Connection conn = dataSource.getConnection()) {
            health.put("database", conn.isValid(1) ? "connected" : "disconnected");
        } catch (Exception e) {
            health.put("database", "error: " + e.getMessage());
        }

        // Redis check
        try {
            redisTemplate.opsForValue().set("health:check", "ok");
            String value = redisTemplate.opsForValue().get("health:check");
            health.put("redis", "ok".equals(value) ? "connected" : "disconnected");
        } catch (Exception e) {
            health.put("redis", "error: " + e.getMessage());
        }

        return ResponseEntity.ok(health);
    }
}
