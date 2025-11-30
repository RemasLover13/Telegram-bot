package com.remaslover.telegrambotaq.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class OpenRouterConfig {

    @Value("${OPENROUTER_API_KEY:}")
    private String apiKey;

    @Value("${spring.ai.openai.base-url:https://openrouter.ai/api/v1}")
    private String baseUrl;

    @Value("${spring.ai.openai.chat.options.model:google/gemini-2.5-flash}")
    private String model;

    @Bean
    @Primary
    public ResponseErrorHandler responseErrorHandler() {
        return new DefaultResponseErrorHandler();
    }

    @Bean
    public OpenAiApi openAiApi(ResponseErrorHandler responseErrorHandler) {
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Authorization", "Bearer " + apiKey);
        headers.add("HTTP-Referer", "https://t.me/OfficialAnswerToQuestionBot");
        headers.add("X-Title", "OfficialAnswerToQuestionBot");

        RestClient.Builder restClientBuilder = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(httpHeaders -> httpHeaders.addAll(headers))
                .defaultStatusHandler(responseErrorHandler);

        WebClient.Builder webClientBuilder = WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(httpHeaders -> httpHeaders.addAll(headers));

        return new OpenAiApi(
                baseUrl,
                () -> apiKey,
                headers,
                "/chat/completions",
                "/embeddings",
                restClientBuilder,
                webClientBuilder,
                responseErrorHandler
        );
    }

    @Bean
    public OpenAiChatOptions openAiChatOptions() {
        return OpenAiChatOptions.builder()
                .model(model)
                .temperature(0.7)
                .maxTokens(500)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatModel chatModel) {
        return ChatClient.builder(chatModel)
                .defaultSystem("""
                        Ты полезный ассистент в Telegram боте. 
                        Отвечай на русском языке кратко и понятно.
                        Будь дружелюбным и помогай пользователям.
                        Если вопрос неясен или требует уточнения - вежливо попроси уточнить.
                        Форматируй ответы для лучшей читаемости.
                        Максимальная длина ответа: 500 символов.
                        """)
                .build();
    }
}