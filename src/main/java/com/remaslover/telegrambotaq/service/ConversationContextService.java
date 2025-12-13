package com.remaslover.telegrambotaq.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.remaslover.telegrambotaq.config.CacheConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Сервис для управления контекстом разговоров с использованием Caffeine Cache.
 * Потокобезопасное хранение истории диалогов пользователей.
 */
@Service
public class ConversationContextService {

    private static final Logger log = LoggerFactory.getLogger(ConversationContextService.class);

    private final Cache<Long, Conversation> conversationCache;
    private final CacheConfig cacheConfig;

    public ConversationContextService(CacheConfig cacheConfig) {
        this.cacheConfig = cacheConfig;

        log.info("Инициализация ConversationContextService");
        log.info(cacheConfig.getConfigSummary());

        this.conversationCache = cacheConfig.<Long, Conversation>createCaffeineBuilder()
                .removalListener(this::onRemoval)
                .build();

        cacheConfig.validate();
        log.info("✅ Кэш успешно инициализирован");
    }

    /**
     * Обработчик удаления записи из кэша
     */
    private void onRemoval(Long userId, Conversation conversation, RemovalCause cause) {
        String causeDescription = switch (cause) {
            case EXPLICIT -> "явное удаление";
            case REPLACED -> "замена новым значением";
            case COLLECTED -> "сборка мусора";
            case EXPIRED -> "истек срок действия";
            case SIZE -> "превышен размер кэша";
        };

        log.debug("🗑️ Контекст удален для userId: {}, причина: {}, сообщений: {}",
                userId, causeDescription,
                conversation != null ? conversation.size() : 0);
    }

    /**
     * Добавляет сообщение пользователя в историю
     */
    public void addUserMessage(Long userId, String message) {
        addMessage(userId, "user", message);
    }

    /**
     * Добавляет ответ ассистента в историю
     */
    public void addAssistantMessage(Long userId, String message) {
        addMessage(userId, "assistant", message);
    }

    /**
     * Основной метод добавления сообщения
     */
    public void addMessage(Long userId, String role, String content) {
        try {
            Conversation conversation = conversationCache.asMap()
                    .computeIfAbsent(userId,
                            key -> {
                                log.debug("Создание нового контекста для userId: {}", key);
                                return new Conversation(cacheConfig.getHistorySize());
                            });

            conversation.addMessage(role, content);

            log.debug("📝 Добавлено сообщение для userId: {}, роль: {}, длина: {}",
                    userId, role, content.length());

        } catch (Exception e) {
            log.error("Ошибка при добавлении сообщения для userId {}: {}",
                    userId, e.getMessage(), e);
            throw new RuntimeException("Не удалось добавить сообщение в контекст", e);
        }
    }

    /**
     * Получает полную историю разговора с системным промптом
     */
    public List<Map<String, String>> getFullConversation(Long userId, String systemPrompt) {
        List<Map<String, String>> fullConversation = new ArrayList<>();

        fullConversation.add(Map.of(
                "role", "system",
                "content", systemPrompt
        ));

        Conversation conversation = conversationCache.getIfPresent(userId);
        if (conversation != null) {
            fullConversation.addAll(conversation.getMessages());
        }

        log.debug("📖 Получена история для userId: {}, сообщений: {}",
                userId, fullConversation.size() - 1);

        return fullConversation;
    }

    /**
     * Получает только историю разговора (без системного промпта)
     */
    public List<Map<String, String>> getConversationHistory(Long userId) {
        Conversation conversation = conversationCache.getIfPresent(userId);
        return conversation != null ? conversation.getMessages() : Collections.emptyList();
    }

    /**
     * Очищает весь кэш (для администратора)
     */
    public void clearAllCache() {
        conversationCache.invalidateAll();
        log.info("🧹 Весь кэш очищен администратором");
    }

    /**
     * Получает историю как строку (для отладки)
     */
    public String getConversationHistoryAsString(Long userId) {
        List<Map<String, String>> history = getConversationHistory(userId);
        StringBuilder sb = new StringBuilder();

        for (Map<String, String> message : history) {
            sb.append(message.get("role"))
                    .append(": ")
                    .append(message.get("content").length() > 100
                            ? message.get("content").substring(0, 100) + "..."
                            : message.get("content"))
                    .append("\n");
        }

        return sb.toString();
    }


    /**
     * Очищает историю для конкретного пользователя
     */
    public void clearHistory(Long userId) {
        conversationCache.invalidate(userId);
        log.info("🧹 Очищена история для userId: {}", userId);
    }

    /**
     * Получает информацию о контексте пользователя
     */
    public Map<String, Object> getUserContextInfo(Long userId) {
        Conversation conversation = conversationCache.getIfPresent(userId);
        Map<String, Object> info = new HashMap<>();

        if (conversation == null) {
            info.put("hasContext", false);
            info.put("message", "Пользователь не имеет активного контекста");
        } else {
            info.put("hasContext", true);
            info.put("messageCount", conversation.size());
            info.put("lastActivity", conversation.getLastActivity());
            info.put("maxHistorySize", cacheConfig.getHistorySize());
        }

        return info;
    }

    /**
     * Получает статистику использования кэша
     */
    public Map<String, Object> getCacheStats() {
        var caffeineStats = conversationCache.stats();
        Map<String, Object> stats = new HashMap<>();

        stats.put("activeUsers", conversationCache.estimatedSize());
        stats.put("totalMessages", getTotalMessagesCount());
        stats.put("cacheHits", caffeineStats.hitCount());
        stats.put("cacheMisses", caffeineStats.missCount());
        stats.put("hitRate", String.format("%.2f%%", caffeineStats.hitRate() * 100));
        stats.put("evictionCount", caffeineStats.evictionCount());
        stats.put("averageLoadPenalty", caffeineStats.averageLoadPenalty());

        stats.put("config", Map.of(
                "maxSize", cacheConfig.getMaxSize(),
                "ttlMinutes", cacheConfig.getTtlMinutes(),
                "historySize", cacheConfig.getHistorySize(),
                "evictionPolicy", cacheConfig.getEvictionPolicy()
        ));

        return stats;
    }

    /**
     * Получает форматированную статистику для отображения
     */
    public String getFormattedStats() {
        Map<String, Object> stats = getCacheStats();
        Map<String, Object> config = (Map<String, Object>) stats.get("config");

        return String.format("""
                        📊 *Статистика контекста:*
                                    
                        • **Активных пользователей:** %d
                        • **Всего сообщений:** %d
                        • **Попаданий в кэш:** %d
                        • **Промахов кэша:** %d
                        • **Эффективность кэша:** %s
                        • **Вытеснено записей:** %d
                                    
                        ⚙️ *Настройки:*
                        • Максимум пользователей: %d
                        • TTL: %d минут
                        • Максимум сообщений на пользователя: %d
                        • Политика вытеснения: %s
                        """,
                stats.get("activeUsers"),
                stats.get("totalMessages"),
                stats.get("cacheHits"),
                stats.get("cacheMisses"),
                stats.get("hitRate"),
                stats.get("evictionCount"),
                config.get("maxSize"),
                config.get("ttlMinutes"),
                config.get("historySize"),
                config.get("evictionPolicy")
        );
    }

    /**
     * Подсчитывает общее количество сообщений во всех контекстах
     */
    private int getTotalMessagesCount() {
        return conversationCache.asMap().values().stream()
                .mapToInt(Conversation::size)
                .sum();
    }

    /**
     * Получает количество активных пользователей
     */
    public int getActiveUsersCount() {
        return (int) conversationCache.estimatedSize();
    }

    /**
     * Проверяет, есть ли у пользователя активный контекст
     */
    public boolean hasActiveContext(Long userId) {
        return conversationCache.getIfPresent(userId) != null;
    }


    /**
     * Класс для хранения разговора пользователя
     * Полностью потокобезопасный
     */
    private static class Conversation {
        private final Deque<Map<String, String>> messages;
        private final int maxSize;
        private volatile LocalDateTime lastActivity;

        public Conversation(int maxSize) {
            this.maxSize = maxSize;
            this.messages = new ConcurrentLinkedDeque<>();
            this.lastActivity = LocalDateTime.now();
        }

        /**
         * Потокобезопасное добавление сообщения
         */
        public void addMessage(String role, String content) {
            Map<String, String> message = Map.of(
                    "role", role,
                    "content", content,
                    "timestamp", LocalDateTime.now().toString()
            );

            messages.addFirst(message);

            if (messages.size() > maxSize) {
                messages.removeLast();
            }

            lastActivity = LocalDateTime.now();
        }

        /**
         * Получает все сообщения в правильном порядке (от старых к новым)
         */
        public List<Map<String, String>> getMessages() {
            List<Map<String, String>> result = new ArrayList<>(messages);
            Collections.reverse(result);
            return result;
        }

        /**
         * Получает количество сообщений
         */
        public int size() {
            return messages.size();
        }

        /**
         * Получает время последней активности
         */
        public LocalDateTime getLastActivity() {
            return lastActivity;
        }
    }
}
