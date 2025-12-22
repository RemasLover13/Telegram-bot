package com.remaslover.telegrambotaq.controller;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("videoId", "Sar0sxF8Umc");
        model.addAttribute("serverTime", LocalDateTime.now().toString());
        return "home";
    }

    @GetMapping("/ping")
    @ResponseBody
    public Map<String, Object> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "Telegram Bot");
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }

    @GetMapping("/health")
    @ResponseBody
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("version", "2.0");
        health.put("keepAlive", "enabled");
        health.put("media", "enabled");
        health.put("youtube_video", "embedded");
        health.put("audio_playlist", "3 tracks");
        return health;
    }
}
