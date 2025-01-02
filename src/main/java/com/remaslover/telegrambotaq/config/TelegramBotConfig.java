package com.remaslover.telegrambotaq.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@PropertySource("classpath:application.yml")
public class TelegramBotConfig {

    @Value("${bot.name}")
    String botName;

    @Value("${bot.token}")
    String botToken;

    @Value("${bot.owner}")
    Long botOwner;

    public TelegramBotConfig(String botName, String botToken, Long botOwner) {
        this.botName = botName;
        this.botToken = botToken;
        this.botOwner = botOwner;
    }

    public TelegramBotConfig() {
    }

    public String getBotName() {
        return this.botName;
    }

    public String getBotToken() {
        return this.botToken;
    }

    public void setBotName(String botName) {
        this.botName = botName;
    }

    public void setBotToken(String botToken) {
        this.botToken = botToken;
    }

    public Long getBotOwner() {
        return botOwner;
    }

    public void setBotOwner(Long botOwner) {
        this.botOwner = botOwner;
    }
}
