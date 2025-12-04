package com.remaslover.telegrambotaq.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.remaslover.telegrambotaq.util.TelegramMarkdownEscapeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenRouterService {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterService.class);

    @Value("${OPENROUTER_API_KEY:}")
    private String apiKey;

    @Value("${OPENROUTER_MODEL:google/gemini-2.5-flash}")
    private String model;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenRouterService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String generateResponse(String userMessage) {
        try {
            log.info("Sending request to OpenRouter: {}", userMessage);

            if (apiKey == null || apiKey.isEmpty()) {
                log.error("OpenRouter API key is not configured");
                return "❌ API ключ OpenRouter не настроен. Обратитесь к администратору.";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("HTTP-Referer", "https://t.me/OfficialAnswerToQuestionBot");
            headers.set("X-Title", "OfficialAnswerToQuestionBot");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", List.of(
                    Map.of("role", "system", "content", """
                            Ты полезный ассистент в Telegram боте. 
                            Отвечай на русском языке кратко и понятно.
                            Будь дружелюбным и помогай пользователям.
                            Если вопрос неясен или требует уточнения - вежливо попроси уточнить.
                            Форматируй ответы для лучшей читаемости.
                            Максимальная длина ответа: 500 символов.
                            """),
                    Map.of("role", "user", "content", userMessage)
            ));
            requestBody.put("max_tokens", 500);
            requestBody.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("Sending HTTP request to OpenRouter with model: {}", model);

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://openrouter.ai/api/v1/chat/completions",
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.path("choices");

                if (choices.isArray() && choices.size() > 0) {
                    String content = choices.get(0)
                            .path("message")
                            .path("content")
                            .asText();

                    log.info("✅ OpenRouter response received: {} characters", content.length());

                    String escapedContent = TelegramMarkdownEscapeUtil.escapeMarkdown(content);
                    log.debug("Escaped content length: {}", escapedContent.length());

                    return escapedContent;
                } else {
                    log.error("❌ No choices in OpenRouter response: {}", response.getBody());
                    return "❌ Ошибка: пустой ответ от AI сервиса";
                }
            } else {
                log.error("❌ OpenRouter API error: {} - {}", response.getStatusCode(), response.getBody());
                return "❌ Ошибка API OpenRouter: " + response.getStatusCode();
            }

        } catch (Exception e) {
            log.error("❌ Error generating AI response: {}", e.getMessage(), e);
            return handleOpenRouterError(e);
        }
    }

    private String handleOpenRouterError(Exception e) {
        String errorMessage = e.getMessage();

        if (errorMessage.contains("400") && errorMessage.contains("not a valid model")) {
            return "❌ Неправильное название модели '" + model + "'. Используйте /models для списка доступных моделей.";
        } else if (errorMessage.contains("404")) {
            return "❌ Модель '" + model + "' не найдена. Используйте /models для списка доступных моделей.";
        } else if (errorMessage.contains("401")) {
            return "🔑 Неверный API ключ OpenRouter. Проверьте настройки.";
        } else if (errorMessage.contains("429")) {
            return "⏳ Превышен лимит запросов. Попробуйте позже.";
        } else {
            return "⚠️ Временная ошибка AI сервиса. Попробуйте позже.";
        }
    }


}