package com.remaslover.telegrambotaq.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private final Map<Long, UserUsage> userUsage = new ConcurrentHashMap<>();
    private static final int DAILY_FREE_LIMIT = 5;


    @Value("${spring.ai.openai.api-key:}")
    private String apiKey;

    @Value("${app.openrouter.site-url:https://t.me/OfficialAnswerToQuestionBot}")
    private String siteUrl;

    @Value("${app.openrouter.app-name:OfficialAnswerToQuestionBot}")
    private String appName;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public RateLimitService() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public boolean canMakeRequest(Long userId) {
        UserUsage usage = userUsage.getOrDefault(userId, new UserUsage());

        if (usage.dailyCount >= DAILY_FREE_LIMIT) {
            log.info("User {} exceeded daily AI limit", userId);
            return false;
        }

        usage.dailyCount++;
        userUsage.put(userId, usage);
        log.debug("User {} AI request count: {}/{}", userId, usage.dailyCount, DAILY_FREE_LIMIT);
        return true;
    }

    // Метод БЕЗ аргументов для команды /credits
    public String getUsageInfo() {
        try {
            if (apiKey == null || apiKey.isEmpty()) {
                return "❌ API ключ OpenRouter не настроен";
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("HTTP-Referer", siteUrl);
            headers.set("X-Title", appName);
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
                sb.append("• **Тип аккаунта:** ").append(isFreeTier ? "Бесплатный 🆓" : "Платный 💰").append("\n");
                sb.append("• **Лейбл:** ").append(label).append("\n");

                if (limit > 0) {
                    double remaining = limit - used;
                    double percentage = (used / limit) * 100;
                    sb.append("• **Использовано:** $").append(String.format("%.4f", used)).append("\n");
                    sb.append("• **Лимит:** $").append(String.format("%.4f", limit)).append("\n");
                    sb.append("• **Осталось:** $").append(String.format("%.4f", remaining)).append("\n");
                    sb.append("• **Заполнено:** ").append(String.format("%.1f", percentage)).append("%\n");
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

    // Дополнительный метод с userId если нужен для других целей
    public String getUsageInfo(Long userId) {
        // Можно добавить логику, связанную с конкретным пользователем
        String generalInfo = getUsageInfo();
        return generalInfo + "\n\n👤 *Запрошено пользователем ID:* " + userId;
    }

    public int getRemainingRequests(Long userId) {
        UserUsage usage = userUsage.getOrDefault(userId, new UserUsage());
        return DAILY_FREE_LIMIT - usage.dailyCount;
    }

    public int getUsedRequests(Long userId) {
        UserUsage usage = userUsage.getOrDefault(userId, new UserUsage());
        return usage.dailyCount;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyCounters() {
        int userCount = userUsage.size();
        userUsage.clear();
        log.info("Daily AI usage counters reset for {} users", userCount);
    }


    public Map<Long, UserUsage> getAllUsage() {
        return new ConcurrentHashMap<>(userUsage);
    }

    public static class UserUsage {
        private int dailyCount = 0;

        public int getDailyCount() {
            return dailyCount;
        }

        public void setDailyCount(int dailyCount) {
            this.dailyCount = dailyCount;
        }
    }
}