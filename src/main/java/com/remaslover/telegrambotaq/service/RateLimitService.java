package com.remaslover.telegrambotaq.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private final Map<Long, UserUsage> userUsage = new ConcurrentHashMap<>();
    private static final int DAILY_FREE_LIMIT = 10;

    /**
     * Отладочный метод - показывает все записи
     */
    public void debugPrintAll() {
        log.info("=== RateLimitService Debug ===");
        log.info("Total users in map: {}", userUsage.size());
        userUsage.forEach((userId, usage) -> {
            log.info("User {}: count={}, lastDate={}",
                    userId, usage.dailyCount, usage.lastRequestDate);
        });
        log.info("=== End Debug ===");
    }

    /**
     * Проверяет, может ли пользователь сделать AI запрос
     */
    public boolean canMakeAiRequest(Long userId) {
        UserUsage usage = getUserUsage(userId);

        if (!usage.lastRequestDate.equals(LocalDate.now())) {
            usage.dailyCount = 0;
            usage.lastRequestDate = LocalDate.now();
            userUsage.put(userId, usage);
            log.debug("New day for user {}, counter reset to 0", userId);
        }

        if (usage.dailyCount >= DAILY_FREE_LIMIT) {
            log.info("User {} exceeded daily AI limit: {}/{}",
                    userId, usage.dailyCount, DAILY_FREE_LIMIT);
            return false;
        }

        log.debug("User {} can make AI request: {}/{}",
                userId, usage.dailyCount, DAILY_FREE_LIMIT);
        return true;
    }

    /**
     * Регистрирует AI запрос пользователя
     */
    public void registerAiRequest(Long userId) {
        UserUsage usage = getUserUsage(userId);

        if (!usage.lastRequestDate.equals(LocalDate.now())) {
            usage.dailyCount = 0;
            usage.lastRequestDate = LocalDate.now();
        }

        if (usage.dailyCount < DAILY_FREE_LIMIT) {
            usage.dailyCount++;
            userUsage.put(userId, usage);
            log.info("✅ AI request registered for user {}: {}/{}",
                    userId, usage.dailyCount, DAILY_FREE_LIMIT);
        } else {
            log.warn("⚠️ Attempt to register AI request for user {} beyond limit: {}/{}",
                    userId, usage.dailyCount, DAILY_FREE_LIMIT);
        }
    }

    /**
     * Получает или создает UserUsage для пользователя
     * ГАРАНТИРУЕТ, что объект добавляется в мапу
     */
    private UserUsage getUserUsage(Long userId) {
        return userUsage.computeIfAbsent(userId,
                key -> {
                    log.debug("Creating new UserUsage for user {}", key);
                    return new UserUsage();
                });
    }

    /**
     * Получает информацию о лимитах пользователя
     */
    public String getUsageInfo(Long userId) {
        UserUsage usage = getUserUsage(userId);


        if (!usage.lastRequestDate.equals(LocalDate.now())) {
            usage.dailyCount = 0;
            usage.lastRequestDate = LocalDate.now();
            userUsage.put(userId, usage);
            log.debug("New day for user {}, counter reset to 0", userId);
        }

        int remaining = DAILY_FREE_LIMIT - usage.dailyCount;

        return """
                🤖 *Ваши лимиты использования AI:*
                            
                • **Использовано сегодня:** %d из 10 запросов
                • **Осталось сегодня:** %d запросов
                            
                💡 Лимиты сбрасываются каждый день в 00:00
                """.formatted(usage.dailyCount, remaining);
    }

    /**
     * Получает количество оставшихся AI запросов
     */
    public int getRemainingAiRequests(Long userId) {
        UserUsage usage = getUserUsage(userId);

        if (!usage.lastRequestDate.equals(LocalDate.now())) {
            usage.dailyCount = 0;
            usage.lastRequestDate = LocalDate.now();
            userUsage.put(userId, usage);
            log.debug("New day for user {}, counter reset to 0", userId);
        }

        return DAILY_FREE_LIMIT - usage.dailyCount;
    }

    /**
     * Получает количество использованных AI запросов
     */
    public int getUsedAiRequests(Long userId) {
        UserUsage usage = getUserUsage(userId);

        if (!usage.lastRequestDate.equals(LocalDate.now())) {
            usage.dailyCount = 0;
            usage.lastRequestDate = LocalDate.now();
            userUsage.put(userId, usage);
            log.debug("New day for user {}, counter reset to 0", userId);
            return 0;
        }

        return usage.dailyCount;
    }

    /**
     * Сбрасывает счетчики использования в полночь
     */
    @Scheduled(cron = "0 0 0 * * ?")
    public synchronized void resetDailyCounters() {
        int userCount = userUsage.size();
        log.info("🔄 Starting daily reset for {} users", userCount);

        userUsage.forEach((userId, usage) -> {
            usage.dailyCount = 0;
            usage.lastRequestDate = LocalDate.now();
            log.debug("Reset counter for user {} to 0", userId);
        });

        log.info("✅ Daily AI usage counters reset for {} users", userCount);
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
     * Сбрасывает лимиты для конкретного пользователя
     */
    public void resetUserLimits(Long userId) {
        userUsage.remove(userId);
        log.info("Лимиты сброшены для пользователя {}", userId);
    }

    /**
     * Отладочный метод для проверки состояния
     */
    public void debugPrintState(Long userId) {
        UserUsage usage = userUsage.get(userId);
        if (usage == null) {
            log.info("DEBUG: User {} not found in userUsage map", userId);
        } else {
            log.info("DEBUG: User {} - count: {}, lastDate: {}, mapSize: {}",
                    userId, usage.dailyCount, usage.lastRequestDate, userUsage.size());
        }
    }


    /**
     * Получает топ пользователей по использованию
     */
    public List<Map<String, Object>> getTopUsersByUsage(int limit) {
        return userUsage.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().dailyCount, e1.getValue().dailyCount))
                .limit(limit)
                .map(entry -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("userId", entry.getKey());
                    info.put("dailyCount", entry.getValue().dailyCount);
                    return info;
                })
                .collect(Collectors.toList());
    }

    /**
     * Внутренний класс для хранения данных использования пользователя
     */
    private static class UserUsage {
        private int dailyCount = 0;
        private LocalDate lastRequestDate = LocalDate.now();

        @Override
        public String toString() {
            return String.format("UserUsage{count=%d, date=%s}", dailyCount, lastRequestDate);
        }
    }
}