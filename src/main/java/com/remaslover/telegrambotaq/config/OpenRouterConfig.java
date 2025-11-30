package com.remaslover.telegrambotaq.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenRouterConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                    Ты полезный ассистент в Telegram боте. 
                    Отвечай на русском языке кратко и понятно.
                    Будь дружелюбным и помогай пользователям.
                    Если вопрос неясен или требует уточнения - вежливо попроси уточнить.
                    Форматируй ответы для лучшей читаемости.
                    """)
                .build();
    }
}