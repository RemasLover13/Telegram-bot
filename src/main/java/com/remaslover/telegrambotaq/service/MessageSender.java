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
     * Надежный метод отправки сообщений
     */
    public void sendMessage(long chatId, String text) {
        if (trySendWithFullEscape(chatId, text)) {
            return;
        }

        if (trySendWithHtml(chatId, text)) {
            return;
        }

        sendPlainText(chatId, text);
    }

    /**
     * Попытка отправки с полным экранированием
     */
    private boolean trySendWithFullEscape(long chatId, String text) {
        try {
            String safeText = TelegramMarkdownEscapeUtil.escapeForTelegram(text);

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(safeText);
            message.setParseMode("MarkdownV2");

            getBot().execute(message);
            log.debug("✅ Message sent with full escape to chat {} ({} chars)",
                    chatId, text.length());
            return true;

        } catch (Exception e) {
            log.debug("Full escape failed for chat {}: {}", chatId, e.getMessage());
            return false;
        }
    }

    /**
     * Попытка отправки с HTML
     */
    private boolean trySendWithHtml(long chatId, String text) {
        try {
            String htmlText = convertToSafeHtml(text);

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(htmlText);
            message.setParseMode("HTML");

            getBot().execute(message);
            log.debug("✅ Message sent with HTML to chat {} ({} chars)",
                    chatId, text.length());
            return true;

        } catch (Exception e) {
            log.debug("HTML failed for chat {}: {}", chatId, e.getMessage());
            return false;
        }
    }

    /**
     * Конвертация в безопасный HTML
     */
    private String convertToSafeHtml(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * Отправляет сообщение как обычный текст
     */
    private void sendPlainText(long chatId, String text) {
        try {
            String plainText = text
                    .replace("\\", "\\\\")
                    .replace("_", "_")
                    .replace("*", "*");

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(plainText);

            getBot().execute(message);
            log.debug("✅ Plain text message sent to chat {}", chatId);

        } catch (Exception e) {
            log.error("❌ Failed to send plain text to chat {}: {}", chatId, e.getMessage());
        }
    }

    public void sendAiResponse(long chatId, String text) {
        try {
            String cleaned = TelegramMarkdownEscapeUtil.cleanAiResponse(text);

            String safeText = TelegramMarkdownEscapeUtil.escapeMinimal(cleaned);

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(safeText);
            message.setParseMode("MarkdownV2");

            getBot().execute(message);
            log.debug("✅ AI response sent to chat {} ({} chars)", chatId, text.length());

        } catch (TelegramApiException e) {
            log.error("❌ Markdown failed, trying plain text: {}", e.getMessage());

            try {
                String cleaned = TelegramMarkdownEscapeUtil.cleanAiResponse(text);

                SendMessage message = new SendMessage();
                message.setChatId(String.valueOf(chatId));
                message.setText(cleaned);

                getBot().execute(message);
                log.debug("✅ AI response sent as plain text to chat {}", chatId);

            } catch (Exception e2) {
                log.error("❌ Complete failure for chat {}: {}", chatId, e2.getMessage());
            }
        }
    }

    /**
     * Отправляет сообщение как обычный текст (без Markdown)
     */
    public void sendPlainTextNoMarkdown(long chatId, String text) {
        try {
            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(text);

            getBot().execute(message);
            log.debug("✅ Plain text (no markdown) sent to chat {}", chatId);

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


