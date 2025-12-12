package com.remaslover.telegrambotaq.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.concurrent.TimeUnit;

@Validated
@Configuration
@ConfigurationProperties(prefix = "context.cache")
public class CacheConfig {
    private int maxSize = 1000;
    private int ttlMinutes = 30;
    private int historySize = 10;
    private boolean enableStats = true;
    private boolean recordStats = true;
    private String evictionPolicy = "size-based";

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be greater than 0");
        }
        this.maxSize = maxSize;
    }

    public int getTtlMinutes() {
        return ttlMinutes;
    }

    public void setTtlMinutes(int ttlMinutes) {
        if (ttlMinutes <= 0) {
            throw new IllegalArgumentException("ttlMinutes must be greater than 0");
        }
        this.ttlMinutes = ttlMinutes;
    }

    public int getHistorySize() {
        return historySize;
    }

    public void setHistorySize(int historySize) {
        if (historySize <= 0) {
            throw new IllegalArgumentException("historySize must be greater than 0");
        }
        this.historySize = historySize;
    }

    public boolean isEnableStats() {
        return enableStats;
    }

    public void setEnableStats(boolean enableStats) {
        this.enableStats = enableStats;
    }

    public boolean isRecordStats() {
        return recordStats;
    }

    public void setRecordStats(boolean recordStats) {
        this.recordStats = recordStats;
    }

    public String getEvictionPolicy() {
        return evictionPolicy;
    }

    public void setEvictionPolicy(String evictionPolicy) {
        this.evictionPolicy = evictionPolicy;
    }

    public Caffeine<Object, Object> createCaffeineBuilder() {
        Caffeine<Object, Object> builder = Caffeine.newBuilder();

        if (maxSize > 0) {
            builder.maximumSize(maxSize);
        }

        if (ttlMinutes > 0) {
            builder.expireAfterAccess(ttlMinutes, TimeUnit.MINUTES);
        }

        if (recordStats) {
            builder.recordStats();
        }

        if ("time-based".equalsIgnoreCase(evictionPolicy)) {
            builder.expireAfterWrite(ttlMinutes * 2L, TimeUnit.MINUTES);
        }

        return builder;
    }

    public String getConfigSummary() {
        return String.format("""
                        Cache Configuration:
                        - Maximum size: %d entries
                        - TTL: %d minutes
                        - History size: %d messages per user
                        - Statistics enabled: %s
                        - Eviction policy: %s
                        """,
                maxSize, ttlMinutes, historySize, enableStats, evictionPolicy
        );
    }

    /**
     * Проверяет валидность конфигурации
     */
    public void validate() {
        if (maxSize <= 0) {
            throw new IllegalStateException("maxSize must be positive");
        }
        if (ttlMinutes <= 0) {
            throw new IllegalStateException("ttlMinutes must be positive");
        }
        if (historySize <= 0) {
            throw new IllegalStateException("historySize must be positive");
        }
    }


    public static Builder builder() {
        return new Builder();
    }


    public static class Builder {
        private int maxSize = 1000;
        private int ttlMinutes = 30;
        private int historySize = 10;
        private boolean enableStats = true;
        private boolean recordStats = true;
        private String evictionPolicy = "size-based";

        private Builder() {
        }

        public Builder maxSize(int maxSize) {
            this.maxSize = maxSize;
            return this;
        }

        public Builder ttlMinutes(int ttlMinutes) {
            this.ttlMinutes = ttlMinutes;
            return this;
        }

        public Builder historySize(int historySize) {
            this.historySize = historySize;
            return this;
        }

        public Builder enableStats(boolean enableStats) {
            this.enableStats = enableStats;
            return this;
        }

        public Builder recordStats(boolean recordStats) {
            this.recordStats = recordStats;
            return this;
        }

        public Builder evictionPolicy(String evictionPolicy) {
            this.evictionPolicy = evictionPolicy;
            return this;
        }

        public CacheConfig build() {
            CacheConfig config = new CacheConfig();
            config.setMaxSize(this.maxSize);
            config.setTtlMinutes(this.ttlMinutes);
            config.setHistorySize(this.historySize);
            config.setEnableStats(this.enableStats);
            config.setRecordStats(this.recordStats);
            config.setEvictionPolicy(this.evictionPolicy);
            config.validate();
            return config;
        }
    }
}
