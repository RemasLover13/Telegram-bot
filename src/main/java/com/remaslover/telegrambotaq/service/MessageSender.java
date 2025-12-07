package com.remaslover.telegrambotaq.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Service
public class MessageSender {
    private final TelegramBotService telegramBotService;

    private static final Logger log = LoggerFactory.getLogger(MessageSender.class);

    public MessageSender(TelegramBotService telegramBotService) {
        this.telegramBotService = telegramBotService;
    }

    public void sendMessage(long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(text);
        sendMessage.setParseMode("Markdown");

        try {
            telegramBotService.execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Error sending message: {}", e.getMessage());
        }
    }

    public void sendMessageWithKeyboard(long chatId, String text, ReplyKeyboardMarkup keyboard) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(text);
        sendMessage.setParseMode("Markdown");
        sendMessage.setReplyMarkup(keyboard);

        try {
            telegramBotService.execute(sendMessage);
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
            telegramBotService.execute(sendMessage);
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
            telegramBotService.execute(message);
        } catch (TelegramApiException e) {
            log.error("Error editing message: {}", e.getMessage());
        }
    }


}
