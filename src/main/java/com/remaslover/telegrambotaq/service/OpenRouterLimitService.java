package com.remaslover.telegrambotaq.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OpenRouterLimitService {

    private static final Logger log = LoggerFactory.getLogger(OpenRouterLimitService.class);

    @Value("${OPENROUTER_API_KEY:}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OpenRouterLimitService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public String getUsageInfo() {
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                return "❌ API ключ OpenRouter не настроен";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("HTTP-Referer", "https://t.me/OfficialAnswerToQuestionBot");
            headers.set("X-Title", "OfficialAnswerToQuestionBot");
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    "https://openrouter.ai/api/v1/auth/key",
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode data = root.path("data");

                double used = data.path("usage").asDouble();
                String label = data.path("label").asText("Не указан");
                double limit = data.path("limit").asDouble(0);
                boolean isFreeTier = data.path("is_free_tier").asBoolean(true);

                StringBuilder sb = new StringBuilder();
                sb.append("📊 *Информация об использовании OpenRouter:*\n\n");
                sb.append("• Тип аккаунта: ").append(isFreeTier ? "Бесплатный 🆓" : "Платный 💰").append("\n");
                sb.append("• Лейбл: ").append(label).append("\n");

                if (limit > 0) {
                    double remaining = limit - used;
                    double percentage = (used / limit) * 100;
                    sb.append("• Использовано: $").append(String.format("%.4f", used)).append("\n");
                    sb.append("• Лимит: $").append(String.format("%.4f", limit)).append("\n");
                    sb.append("• Осталось: $").append(String.format("%.4f", remaining)).append("\n");
                    sb.append("• Заполнено: ").append(String.format("%.1f", percentage)).append("%\n");
                } else {
                    sb.append("• **Использовано:** $").append(String.format("%.4f", used)).append("\n");
                    sb.append("• **Лимит:** не установлен\n");
                }

                return sb.toString();
            } else {
                return "⚠️ Ошибка при запросе к OpenRouter API: " + response.getStatusCode();
            }

        } catch (Exception e) {
            log.error("Error fetching OpenRouter usage: {}", e.getMessage(), e);
            return "⚠️ Не удалось получить информацию об использовании OpenRouter. Проверьте API ключ.";
        }
    }
}