package com.remaslover.telegrambotaq.service;

import com.remaslover.telegrambotaq.config.TelegramBotConfig;
import com.remaslover.telegrambotaq.util.TelegramMarkdownEscapeUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Service
public class MessageSender {

    private final ApplicationContext applicationContext;
    private final TelegramBotConfig botConfig;

    private static final Logger log = LoggerFactory.getLogger(MessageSender.class);

    public MessageSender(ApplicationContext applicationContext, TelegramBotConfig botConfig) {
        this.applicationContext = applicationContext;
        this.botConfig = botConfig;
    }

    private TelegramLongPollingBot getBot() {
        return applicationContext.getBean(TelegramLongPollingBot.class);
    }

    public void sendMessage(long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));

        try {
            sendMessage.setText(text);
            sendMessage.setParseMode("Markdown");
            getBot().execute(sendMessage);

        } catch (TelegramApiException e) {
            log.warn("Markdown parsing failed for chat {}: {}", chatId, e.getMessage());

            try {
                sendMessage.setParseMode(null);

                String safeText = TelegramMarkdownEscapeUtil.escapeMinimal(text);
                sendMessage.setText(safeText);

                getBot().execute(sendMessage);

            } catch (TelegramApiException e2) {
                log.error("Error sending plain message to chat {}: {}", chatId, e2.getMessage());

                try {
                    String[] chunks = splitMessage(text, 2000);
                    for (String chunk : chunks) {
                        SendMessage chunkMessage = new SendMessage();
                        chunkMessage.setChatId(String.valueOf(chatId));
                        chunkMessage.setText(chunk);
                        chunkMessage.setParseMode(null);
                        getBot().execute(chunkMessage);
                        Thread.sleep(100);
                    }

                } catch (Exception e3) {
                    log.error("All sending attempts failed for chat {}: {}", chatId, e3.getMessage());
                }
            }
        }
    }

    private String[] splitMessage(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return new String[]{text};
        }

        List<String> parts = new ArrayList<>();
        int start = 0;

        while (start < text.length()) {
            int end = Math.min(start + maxLength, text.length());

            if (end < text.length()) {
                int lastSpace = text.lastIndexOf('\n', end);
                if (lastSpace > start + maxLength / 2) {
                    end = lastSpace;
                }
            }

            parts.add(text.substring(start, end));
            start = end;
        }

        for (int i = 0; i < parts.size(); i++) {
            parts.set(i, String.format("(%d/%d)\n%s", i + 1, parts.size(), parts.get(i)));
        }

        return parts.toArray(new String[0]);
    }

    public void sendMessageWithKeyboard(long chatId, String text, ReplyKeyboardMarkup keyboard) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(text);
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(keyboard);

        try {
            getBot().execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Error sending message with keyboard: {}", e.getMessage());
        }
    }

    public void sendMessageWithInlineKeyboard(long chatId, String text, InlineKeyboardMarkup keyboard) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(text);
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(keyboard);

        try {
            getBot().execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Error sending message with inline keyboard: {}", e.getMessage());
        }
    }

    public void editMessage(long chatId, int messageId, String newText) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(newText);
        message.setMessageId(messageId);

        try {
            getBot().execute(message);
        } catch (TelegramApiException e) {
            log.error("Error editing message: {}", e.getMessage());
        }
    }
}