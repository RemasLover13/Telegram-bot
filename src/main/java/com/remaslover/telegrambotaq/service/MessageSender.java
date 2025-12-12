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

    public void sendMessageWithInlineKeyboard(long chatId, String text, InlineKeyboardMarkup keyboard) {
        try {
            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(String.valueOf(chatId));

            String escapedText = TelegramMarkdownEscapeUtil.escapeMarkdownV2(text);
            sendMessage.setText(escapedText);
            sendMessage.setParseMode("MarkdownV2");
            sendMessage.setReplyMarkup(keyboard);

            try {
                getBot().execute(sendMessage);
                log.debug("✅ Message with inline keyboard sent to chat {} using MarkdownV2", chatId);

            } catch (TelegramApiException e) {
                log.warn("MarkdownV2 failed for inline keyboard, trying HTML: {}", e.getMessage());

                sendMessage.setText(TelegramMarkdownEscapeUtil.escapeHtml(text));
                sendMessage.setParseMode("HTML");
                getBot().execute(sendMessage);
                log.debug("✅ Message with inline keyboard sent to chat {} using HTML", chatId);
            }

        } catch (Exception e) {
            log.error("Error sending message with inline keyboard: {}", e.getMessage(), e);

            sendMessage(chatId, text);
        }
    }

    public void sendMessage(long chatId, String text) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(text);

            message.setParseMode("MarkdownV2");

            try {
                getBot().execute(message);
                log.debug("✅ Message sent with MarkdownV2 to chat {} ({} chars)",
                        chatId, text.length());

            } catch (TelegramApiException e) {
                log.warn("MarkdownV2 failed for chat {}, trying HTML: {}",
                        chatId, e.getMessage());

                message.setParseMode("HTML");
                getBot().execute(message);
                log.debug("✅ Message sent with HTML to chat {}", chatId);

            }

        } catch (Exception e) {
            log.error("❌ Failed to send message to chat {}: {}", chatId, e.getMessage(), e);
        }
    }

    private void sendMessageWithParseMode(long chatId, String text, String parseMode) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(text);

            if (parseMode != null) {
                message.setParseMode(parseMode);
            }

            getBot().execute(message);

        } catch (Exception e) {
            log.error("Failed to send plain message to chat {}: {}", chatId, e.getMessage());
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