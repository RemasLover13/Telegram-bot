package com.remaslover.telegrambotaq.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OpenRouterService {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterService.class);

    private final ChatClient chatClient;

    public OpenRouterService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String generateResponse(String userMessage) {
        try {
            return chatClient.prompt()
                    .system(s -> s.text("""
                    Ты полезный ассистент в Telegram боте. 
                    Отвечай на русском языке кратко и понятно.
                    Будь дружелюбным и помогай пользователям.
                    Если вопрос неясен или требует уточнения - вежливо попроси уточнить.
                    Форматируй ответы для лучшей читаемости.
                    """))
                    .user(userMessage)
                    .call()
                    .content();

        } catch (Exception e) {
            log.error("Error generating AI response: {}", e.getMessage());
            return "Извините, произошла ошибка при обработке запроса. Попробуйте позже.\n\nОшибка: " + e.getMessage();
        }
    }

    public String generateResponseWithContext(String userMessage, String context) {
        try {
            return chatClient.prompt()
                    .system(s -> s.text("""
                    Ты полезный ассистент в Telegram боте. 
                    Отвечай на русском языке кратко и понятно.
                    Контекст предыдущего разговора: %s
                    """.formatted(context)))
                    .user(userMessage)
                    .call()
                    .content();

        } catch (Exception e) {
            log.error("Error generating AI response with context: {}", e.getMessage());
            return "Извините, произошла ошибка при обработке запроса с контекстом.";
        }
    }

    public String generateCreativeContent(String prompt) {
        try {
            return chatClient.prompt()
                    .system(s -> s.text("""
                    Ты креативный помощник. Создавай интересный контент на русском языке.
                    Будь оригинальным, творческим и engaging.
                    Используй эмодзи где это уместно.
                    """))
                    .user(prompt)
                    .call()
                    .content();

        } catch (Exception e) {
            log.error("Error generating creative content: {}", e.getMessage());
            return "Извините, не удалось сгенерировать креативный контент. Попробуйте позже.";
        }
    }

    public String generateJoke(String topic) {
        try {
            return chatClient.prompt()
                    .system(s -> s.text("""
                    Ты профессиональный комик. Создавай смешные шутки на русском языке.
                    Шутки должны быть уместными, смешными и оригинальными.
                    Если тема не указана - придумай шутку на случайную тему.
                    """))
                    .user("Придумай шутку на тему: " + topic)
                    .call()
                    .content();

        } catch (Exception e) {
            log.error("Error generating joke: {}", e.getMessage());
            return "Извините, не удалось придумать шутку. Попробуйте позже.";
        }
    }

    // Метод для тестирования подключения
    public String testConnection() {
        try {
            String response = chatClient.prompt()
                    .user("Ответь кратко: 'Соединение установлено успешно' на русском")
                    .call()
                    .content();
            log.info("OpenRouter connection test successful");
            return "✅ " + response;
        } catch (Exception e) {
            log.error("OpenRouter connection test failed: {}", e.getMessage());
            return "❌ Ошибка подключения к OpenRouter: " + e.getMessage();
        }
    }
}