package com.remaslover.telegrambotaq.config;


import com.remaslover.telegrambotaq.service.TelegramBotService;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class BotInitializer {

    private final TelegramBotService telegramBotService;
    private static final Logger log = LoggerFactory.getLogger(BotInitializer.class);

    public BotInitializer(TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    @EventListener(ContextRefreshedEvent.class)
    public void init() {

        try {
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(telegramBotService);
        } catch (TelegramApiException e) {
            log.error("Error initializing Bot: {}", e.getMessage());
        }

    }
}
