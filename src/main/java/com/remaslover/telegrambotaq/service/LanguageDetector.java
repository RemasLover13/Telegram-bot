package com.remaslover.telegrambotaq.service;

import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LanguageDetector {
    public String detectLanguage(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "en";
        }

        String cleanText = text.toLowerCase().trim();

        if (cleanText.matches(".*[а-яё].*")) {
            return "ru";
        }

        if (cleanText.matches(".*[\\u4e00-\\u9fff].*")) {
            return "zh";
        }

        if (cleanText.matches(".*[\\u0600-\\u06FF].*")) {
            return "ar";
        }

        return "en";
    }

    public String getLanguageName(String code) {
        Map<String, String> languages = Map.of(
                "ru", "Russian",
                "en", "English",
                "es", "Spanish",
                "fr", "French",
                "de", "German",
                "zh", "Chinese",
                "ar", "Arabic",
                "ja", "Japanese",
                "ko", "Korean"
        );

        return languages.getOrDefault(code, "English");
    }
}
