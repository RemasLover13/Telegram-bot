package com.remaslover.telegrambotaq.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private final Map<Long, UserUsage> userUsage = new ConcurrentHashMap<>();
    private static final int DAILY_FREE_LIMIT = 5;

    /**
     * Проверяет, может ли пользователь сделать AI запрос
     */
    public boolean canMakeAiRequest(Long userId) {
        UserUsage usage = userUsage.getOrDefault(userId, new UserUsage());

        if (usage.dailyCount >= DAILY_FREE_LIMIT) {
            log.info("User {} exceeded daily AI limit", userId);
            return false;
        }

        log.info("User {} can make AI request: {}/{}",
                userId, usage.dailyCount, DAILY_FREE_LIMIT);
        return true;
    }

    /**
     * Регистрирует AI запрос пользователя
     */
    public void registerAiRequest(Long userId) {
        UserUsage usage = userUsage.computeIfAbsent(userId, k -> new UserUsage());

        if (usage.dailyCount < DAILY_FREE_LIMIT) {
            usage.dailyCount++;
            userUsage.put(userId, usage);
            log.info("AI request registered for user {}: {}/{}",
                    userId, usage.dailyCount, DAILY_FREE_LIMIT);
        } else {
            log.warn("Attempt to register AI request for user {} beyond limit", userId);
        }
    }

    /**
     * Получает информацию о лимитах пользователя
     */
    public String getUsageInfo(Long userId) {
        UserUsage usage = userUsage.getOrDefault(userId, new UserUsage());
        int remaining = DAILY_FREE_LIMIT - usage.dailyCount;

        return """
                🤖 *Ваши лимиты использования AI:*
                            
                • **Использовано сегодня:** %d из 5 запросов
                • **Осталось сегодня:** %d запросов
                            
                💡 Лимиты сбрасываются каждый день в 00:00
                """.formatted(usage.dailyCount, remaining);
    }

    /**
     * Получает количество оставшихся AI запросов
     */
    public int getRemainingAiRequests(Long userId) {
        UserUsage usage = userUsage.getOrDefault(userId, new UserUsage());
        return DAILY_FREE_LIMIT - usage.dailyCount;
    }

    /**
     * Получает количество использованных AI запросов
     */
    public int getUsedAiRequests(Long userId) {
        UserUsage usage = userUsage.getOrDefault(userId, new UserUsage());
        return usage.dailyCount;
    }

    /**
     * Сбрасывает счетчики использования в полночь
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyCounters() {
        int userCount = userUsage.size();
        userUsage.clear();
        log.info("Daily AI usage counters reset for {} users", userCount);
    }

    /**
     * Получает статистику по всем пользователям
     */
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        int totalUsers = userUsage.size();
        int activeUsers = (int) userUsage.values().stream()
                .filter(usage -> usage.dailyCount > 0)
                .count();
        int totalRequests = userUsage.values().stream()
                .mapToInt(usage -> usage.dailyCount)
                .sum();

        stats.put("totalUsers", totalUsers);
        stats.put("activeUsers", activeUsers);
        stats.put("totalRequests", totalRequests);
        stats.put("dailyLimit", DAILY_FREE_LIMIT);

        return stats;
    }

    /**
     * Внутренний класс для хранения данных использования пользователя
     */
    private static class UserUsage {
        private int dailyCount = 0;
        private LocalDate lastRequestDate = LocalDate.now();

        public int getDailyCount() {
            if (!lastRequestDate.equals(LocalDate.now())) {
                dailyCount = 0;
                lastRequestDate = LocalDate.now();
            }
            return dailyCount;
        }

        public void increment() {
            if (!lastRequestDate.equals(LocalDate.now())) {
                dailyCount = 0;
                lastRequestDate = LocalDate.now();
            }
            dailyCount++;
        }
    }
}