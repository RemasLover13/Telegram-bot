package com.remaslover.telegrambotaq.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);

    private final Map<Long, UserUsage> userUsage = new ConcurrentHashMap<>();
    private static final int DAILY_FREE_LIMIT = 5;

    public boolean canMakeAiRequest(Long userId) {
        UserUsage usage = userUsage.getOrDefault(userId, new UserUsage());

        if (usage.dailyCount >= DAILY_FREE_LIMIT) {
            log.info("User {} exceeded daily AI limit", userId);
            return false;
        }

        usage.dailyCount++;
        userUsage.put(userId, usage);
        log.info("User {} AI request count: {}/{}", userId, usage.dailyCount, DAILY_FREE_LIMIT);
        return true;
    }

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


    public int getRemainingAiRequests(Long userId) {
        UserUsage usage = userUsage.getOrDefault(userId, new UserUsage());
        return DAILY_FREE_LIMIT - usage.dailyCount;
    }

    @Scheduled(cron = "0 0 0 * * ?")
    public void resetDailyCounters() {
        int userCount = userUsage.size();
        userUsage.clear();
        log.info("Daily AI usage counters reset for {} users", userCount);
    }

    public static class UserUsage {
        private int dailyCount = 0;

        public int getDailyCount() {
            return dailyCount;
        }
    }
}