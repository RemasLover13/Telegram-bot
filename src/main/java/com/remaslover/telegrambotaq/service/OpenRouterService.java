package com.remaslover.telegrambotaq.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.remaslover.telegrambotaq.util.TelegramMarkdownEscapeUtil;
import com.remaslover.telegrambotaq.util.TelegramMessageSplitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
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
    private final TelegramMessageSplitter telegramMessageSplitter;

    public OpenRouterService(ConversationContextService conversationContextService, TelegramMessageSplitter telegramMessageSplitter) {
        this.telegramMessageSplitter = telegramMessageSplitter;
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
        this.conversationContextService = conversationContextService;
    }

    /**
     * Генерирует ответ с учетом контекста разговора и разбивкой на части
     * (Новый метод, возвращающий список частей)
     */
    public List<String> generateResponseAsParts(Long userId, String userMessage) {
        try {
            log.info("Sending request to OpenRouter for user {}: {}", userId, userMessage);

            if (apiKey == null || apiKey.isEmpty()) {
                log.error("OpenRouter API key is not configured");
                return List.of("❌ API ключ OpenRouter не настроен. Обратитесь к администратору.");
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
            requestBody.put("max_tokens", 2000);
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

                    List<String> messageParts = splitMessageForTelegram(content);

                    log.info("Split response into {} parts for user {}", messageParts.size(), userId);

                    return messageParts;

                } else {
                    log.error("❌ No choices in OpenRouter response: {}", response.getBody());
                    return List.of("❌ Ошибка: пустой ответ от AI сервиса");
                }
            } else {
                log.error("❌ OpenRouter API error: {} - {}", response.getStatusCode(), response.getBody());
                return List.of("❌ Ошибка API OpenRouter: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("❌ Error generating AI response for user {}: {}", userId, e.getMessage(), e);
            return List.of(handleOpenRouterError(e));
        }
    }

    /**
     * Разбивает длинное сообщение на части для Telegram
     */
    private List<String> splitMessageForTelegram(String text) {
        List<String> parts = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return parts;
        }

        String safeText = TelegramMarkdownEscapeUtil.escapeMarkdownSmart(text);

        int maxLength = 3500;

        if (safeText.length() <= maxLength) {
            parts.add(safeText);
            return parts;
        }

        String[] paragraphs = safeText.split("\n\n");

        StringBuilder currentPart = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (currentPart.length() + paragraph.length() + 20 > maxLength && currentPart.length() > 0) {
                parts.add(currentPart.toString());

                currentPart = new StringBuilder();
            }

            if (currentPart.length() > 0) {
                currentPart.append("\n\n");
            }
            currentPart.append(paragraph);
        }

        if (currentPart.length() > 0) {
            parts.add(currentPart.toString());
        }

        return parts;
    }

    /**
     * Старый метод для обратной совместимости
     */
    public String generateResponse(Long userId, String userMessage) {
        try {
            List<String> parts = generateResponseAsParts(userId, userMessage);
            if (parts.isEmpty()) {
                return "Пустой ответ от AI";
            } else if (parts.size() == 1) {
                return parts.get(0);
            } else {
                return parts.get(0) + "\n\n📄 *Продолжение следует в следующем сообщении...*";
            }
        } catch (Exception e) {
            log.error("Error in generateResponse: {}", e.getMessage(), e);
            return "Ошибка при генерации ответа";
        }
    }

    /**
     * Оценивает общее количество частей
     */
    private int estimateTotalParts(String text, int maxLength) {
        return (int) Math.ceil((double) text.length() / maxLength);
    }

    /**
     * Форматирует часть сообщения с нумерацией
     */
    private String formatMessagePart(int partNumber, int totalParts, String content) {
        if (totalParts <= 1) {
            return content;
        }

        String header = String.format("📄 *Часть %d из %d:*\n\n", partNumber, totalParts);
        String footer = String.format("\n\n_Продолжение следует... (%d/%d)_", partNumber, totalParts);

        return header + content + footer;
    }

    /**
     * Альтернативный метод разбивки по предложениям
     */
    private List<String> splitBySentences(String text, int maxLength) {
        List<String> parts = new ArrayList<>();

        if (text.length() <= maxLength) {
            parts.add(text);
            return parts;
        }

        String[] sentences = text.split("(?<=[.!?])\\s+");

        StringBuilder currentPart = new StringBuilder();

        for (String sentence : sentences) {
            if (currentPart.length() + sentence.length() + 1 > maxLength && !currentPart.isEmpty()) {
                parts.add(currentPart.toString());
                currentPart = new StringBuilder();
            }

            if (!currentPart.isEmpty()) {
                currentPart.append(" ");
            }
            currentPart.append(sentence);
        }

        if (!currentPart.isEmpty()) {
            parts.add(currentPart.toString());
        }

        return parts;
    }

    /**
     * Разбивает на фиксированные части
     */
    private List<String> splitIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();

        for (int i = 0; i < text.length(); i += chunkSize) {
            int end = Math.min(text.length(), i + chunkSize);

            if (end < text.length() && !Character.isWhitespace(text.charAt(end))) {
                int lastSpace = text.lastIndexOf(' ', end);
                if (lastSpace > i + chunkSize / 2) {
                    end = lastSpace;
                }
            }

            chunks.add(text.substring(i, end).trim());

            if (end < text.length() && Character.isWhitespace(text.charAt(end))) {
                i = end;
            } else {
                i = end - chunkSize;
            }
        }

        return chunks;
    }

    /**
     * Очищает историю разговора для пользователя
     */
    public void clearConversationHistory(Long userId) {
        conversationContextService.clearHistory(userId);
        log.info("Cleared conversation history for user {}", userId);
    }

    /**
     * Получает историю разговора с правильным экранированием
     */
    public String getConversationHistory(Long userId) {
        List<Map<String, String>> history =
                conversationContextService.getConversationHistory(userId);

        StringBuilder sb = new StringBuilder();

        if (history.isEmpty()) {
            return "📜 *История разговора:*\n\nИстория пуста";
        }

        sb.append("*📜 История разговора:*\n\n");

        int counter = 1;
        for (Map<String, String> message : history) {
            String role = message.get("role");
            String content = message.get("content");

            String roleEmoji = role.equals("user") ? "👤" : "🤖";
            String roleText = role.equals("user") ? "Вы" : "Бот";

            String safeContent = TelegramMarkdownEscapeUtil.escapeMarkdownSmart(content);

            String preview;
            if (safeContent.length() > 100) {
                preview = safeContent.substring(0, 100) + "...";
            } else {
                preview = safeContent;
            }

            preview = preview.replace("\n", " ");

            sb.append(counter)
                    .append(". ")
                    .append(roleEmoji)
                    .append(" *")
                    .append(roleText)
                    .append("*: ")
                    .append(preview)
                    .append("\n\n");

            counter++;
        }

        sb.append("_Всего сообщений: ").append(history.size()).append("_");

        return TelegramMarkdownEscapeUtil.escapeMarkdownSmart(sb.toString());
    }

    /**
     * Метод для отладки - показывает в блоке кода
     */
    public String getConversationHistoryDebug(Long userId) {
        List<Map<String, String>> history =
                conversationContextService.getConversationHistory(userId);

        if (history.isEmpty()) {
            return "```\nИстория разговора пуста\n```";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("```\n");

        int counter = 1;
        for (Map<String, String> message : history) {
            String role = message.get("role");
            String content = message.get("content");

            String roleText = role.equals("user") ? "[ПОЛЬЗОВАТЕЛЬ]" : "[БОТ]";

            sb.append(counter)
                    .append(". ")
                    .append(roleText)
                    .append(":\n")
                    .append(content.length() > 60 ? content.substring(0, 60) + "..." : content)
                    .append("\n")
                    .append("-".repeat(40))
                    .append("\n");

            counter++;
        }

        sb.append("\nВсего: ").append(history.size()).append(" сообщений\n");
        sb.append("```");

        return sb.toString();
    }

    /**
     * Альтернативный безопасный метод (без Markdown)
     */
    public String getConversationHistorySimple(Long userId) {
        List<Map<String, String>> history =
                conversationContextService.getConversationHistory(userId);

        if (history.isEmpty()) {
            return "📜 История разговора:\n\nИстория пуста";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📜 История разговора:\n\n");

        int counter = 1;
        for (Map<String, String> message : history) {
            String role = message.get("role");
            String content = message.get("content");

            String roleText = role.equals("user") ? "👤 Вы" : "🤖 Бот";

            String cleanContent = TelegramMarkdownEscapeUtil.escapeMinimal(content);

            String preview;
            if (cleanContent.length() > 80) {
                preview = cleanContent.substring(0, 80) + "...";
            } else {
                preview = cleanContent;
            }

            preview = preview.replace("\n", " ");

            sb.append(counter)
                    .append(". ")
                    .append(roleText)
                    .append(": ")
                    .append(preview)
                    .append("\n\n");

            counter++;
        }

        sb.append("Всего: ").append(history.size()).append(" сообщений");

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