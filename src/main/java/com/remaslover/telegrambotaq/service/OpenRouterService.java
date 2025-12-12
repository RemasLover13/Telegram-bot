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
    private final ConversationContextService conversationContextService;

    public OpenRouterService(ConversationContextService conversationContextService) {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.conversationContextService = conversationContextService;
    }

    /**
     * Генерирует ответ с учетом контекста разговора
     */
    public String generateResponse(Long userId, String userMessage) {
        try {
            log.info("Sending request to OpenRouter for user {}: {}", userId, userMessage);

            if (apiKey == null || apiKey.isEmpty()) {
                log.error("OpenRouter API key is not configured");
                return "❌ API ключ OpenRouter не настроен. Обратитесь к администратору.";
            }

            conversationContextService.addUserMessage(userId, userMessage);

            List<Map<String, String>> conversationHistory =
                    conversationContextService.getFullConversation(userId, getSystemPrompt());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("HTTP-Referer", "https://t.me/OfficialAnswerToQuestionBot");
            headers.set("X-Title", "OfficialAnswerToQuestionBot");

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", conversationHistory);
            requestBody.put("max_tokens", 500);
            requestBody.put("temperature", 0.7);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            log.info("Sending HTTP request to OpenRouter with {} messages", conversationHistory.size());

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

                    log.info("✅ OpenRouter response received for user {}: {} characters",
                            userId, content.length());

                    conversationContextService.addAssistantMessage(userId, content);

                    String escapedContent = TelegramMarkdownEscapeUtil.escapeMarkdownPreserveCode(content);
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
            log.error("❌ Error generating AI response for user {}: {}", userId, e.getMessage(), e);
            return handleOpenRouterError(e);
        }
    }

    /**
     * Старая версия для обратной совместимости
     */
    @Deprecated
    public String generateResponse(String userMessage) {
        return generateResponse(0L, userMessage);
    }

    /**
     * Очищает историю разговора для пользователя
     */
    public void clearConversationHistory(Long userId) {
        conversationContextService.clearHistory(userId);
        log.info("Cleared conversation history for user {}", userId);
    }

    /**
     * Получает историю разговора в читаемом формате
     */
    public String getConversationHistory(Long userId) {
        List<Map<String, String>> history =
                conversationContextService.getConversationHistory(userId);

        StringBuilder sb = new StringBuilder();
        sb.append("📜 *История разговора:*\n\n");

        if (history.isEmpty()) {
            sb.append("История пуста");
        } else {
            for (Map<String, String> message : history) {
                String role = message.get("role");
                String content = message.get("content");
                String timestamp = message.getOrDefault("timestamp", "");

                String roleEmoji = role.equals("user") ? "👤" : "🤖";
                String roleText = role.equals("user") ? "Вы" : "Бот";

                String preview = content.length() > 80
                        ? content.substring(0, 80) + "..."
                        : content;

                sb.append(roleEmoji)
                        .append(" *")
                        .append(roleText)
                        .append("*: ")
                        .append(preview.replace("\n", " "))
                        .append("\n\n");
            }
        }

        return sb.toString();
    }

    /**
     * Получает статистику контекста
     */
    public String getContextStats() {
        return conversationContextService.getFormattedStats();
    }

    private String getSystemPrompt() {
        return """
                Ты полезный ассистент в Telegram боте. 
                Отвечай на русском языке кратко и понятно.
                Будь дружелюбным и помогай пользователям.
                Если вопрос неясен или требует уточнения - вежливо попроси уточнить.
                Форматируй ответы для лучшей читаемости.
                Максимальная длина ответа: 500 символов.
                Помни контекст разговора и учитывай предыдущие сообщения.
                """;
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