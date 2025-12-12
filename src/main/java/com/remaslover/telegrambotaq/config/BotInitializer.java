package com.remaslover.telegrambotaq.config;


import com.remaslover.telegrambotaq.service.TelegramBotService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Component
public class BotInitializer {

    private static final Logger log = LoggerFactory.getLogger(BotInitializer.class);

    private final TelegramBotService bot;

    public BotInitializer(TelegramBotService bot) {
        this.bot = bot;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void init() {
        log.info("Initializing Telegram Bot...");

        TelegramBotsApi botsApi;
        try {
            botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
            log.info("✅ Bot registered successfully!");

        } catch (TelegramApiException e) {
            log.error("❌ Failed to register bot: {}", e.getMessage());

            if (e.getMessage().contains("terminated by other getUpdates")) {
                handleBotConflict();
            } else {
                log.error("Other Telegram API error: ", e);
            }
        }
    }

    private void handleBotConflict() {
        log.warn("🔄 Bot conflict detected. Waiting and retrying...");

        try {
            Thread.sleep(10000);

            TelegramBotsApi retryApi = new TelegramBotsApi(DefaultBotSession.class);
            retryApi.registerBot(bot);
            log.info("✅ Bot successfully registered after retry!");

        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            log.error("Retry interrupted: ", ie);
        } catch (TelegramApiException e) {
            log.error("❌ Failed to register bot after retry: {}", e.getMessage());
        }
    }
}