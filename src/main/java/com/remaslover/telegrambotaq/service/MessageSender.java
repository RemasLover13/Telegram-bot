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

@Service
public class MessageSender {

    private final ApplicationContext applicationContext;
    private final TelegramBotConfig botConfig;

    private static final Logger log = LoggerFactory.getLogger(MessageSender.class);

    public MessageSender(ApplicationContext applicationContext, TelegramBotConfig botConfig) {
        this.applicationContext = applicationContext;
        this.botConfig = botConfig;
    }

    public TelegramLongPollingBot getBot() {
        return applicationContext.getBean(TelegramLongPollingBot.class);
    }

    /**
     * Основной метод отправки сообщений
     * Использует упрощенное гарантированно безопасное экранирование
     */
    public void sendMessage(long chatId, String text) {
        try {
            String safeText = TelegramMarkdownEscapeUtil.escapeForTelegram(text);

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(safeText);
            message.setParseMode("MarkdownV2");

            getBot().execute(message);
            log.debug("✅ Message sent to chat {} ({} chars)", chatId, text.length());

        } catch (TelegramApiException e) {
            log.warn("MarkdownV2 failed for chat {}, trying plain text: {}",
                    chatId, e.getMessage());

            sendPlainText(chatId, text);

        } catch (Exception e) {
            log.error("❌ Failed to send message to chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    /**
     * Отправляет сообщение как обычный текст (без форматирования)
     */
    private void sendPlainText(long chatId, String text) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(text);

            getBot().execute(message);
            log.debug("✅ Plain text message sent to chat {}", chatId);

        } catch (Exception e) {
            log.error("❌ Failed to send plain text to chat {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Отправляет сообщение с inline клавиатурой
     */
    public void sendMessageWithInlineKeyboard(long chatId, String text, InlineKeyboardMarkup keyboard) {
        try {
            String safeText = TelegramMarkdownEscapeUtil.escapeForTelegram(text);

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(safeText);
            message.setParseMode("MarkdownV2");
            message.setReplyMarkup(keyboard);

            getBot().execute(message);
            log.debug("✅ Message with inline keyboard sent to chat {}", chatId);

        } catch (Exception e) {
            log.error("Error sending message with inline keyboard: {}", e.getMessage(), e);

            sendMessage(chatId, text);
        }
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


