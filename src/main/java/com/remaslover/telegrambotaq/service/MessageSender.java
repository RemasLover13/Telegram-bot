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
     * Надежный метод отправки сообщений с попытками разных форматов
     */
    public void sendMessage(long chatId, String text) {
        if (trySendWithMarkdownV2(chatId, text)) {
            return;
        }

        if (trySendWithHtml(chatId, text)) {
            return;
        }

        sendPlainText(chatId, text);
    }

    /**
     * Попытка отправки с MarkdownV2
     */
    private boolean trySendWithMarkdownV2(long chatId, String text) {
        try {
            String safeText = TelegramMarkdownEscapeUtil.escapeAllMarkdownChars(text);

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(safeText);
            message.setParseMode("MarkdownV2");

            getBot().execute(message);
            log.debug("✅ Message sent with MarkdownV2 to chat {} ({} chars)",
                    chatId, text.length());
            return true;

        } catch (Exception e) {
            log.debug("MarkdownV2 failed for chat {}: {}", chatId, e.getMessage());
            return false;
        }
    }

    /**
     * Попытка отправки с HTML
     */
    private boolean trySendWithHtml(long chatId, String text) {
        try {
            String htmlText = TelegramMarkdownEscapeUtil.convertToHtml(text);

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
     * Отправляет сообщение как обычный текст
     */
    private void sendPlainText(long chatId, String text) {
        try {
            // Минимальное экранирование для plain text
            String plainText = text
                    .replace("\\", "\\\\")
                    .replace("_", "_")
                    .replace("*", "*")
                    .replace("[", "[")
                    .replace("]", "]");

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(plainText);
            // Без parseMode - обычный текст

            getBot().execute(message);
            log.debug("✅ Plain text message sent to chat {}", chatId);

        } catch (Exception e) {
            log.error("❌ Failed to send plain text to chat {}: {}", chatId, e.getMessage());
        }
    }

    /**
     * Специальный метод для отправки AI-ответов
     */
    public void sendAiResponse(long chatId, String text) {
        try {
            // Пробуем разные форматы для AI-ответов
            if (trySendAiWithMarkdown(chatId, text)) {
                return;
            }

            if (trySendAiWithHtml(chatId, text)) {
                return;
            }

            // Fallback на обычную отправку
            sendMessage(chatId, text);

        } catch (Exception e) {
            log.error("❌ Failed to send AI response to chat {}: {}", chatId, e.getMessage(), e);
            sendMessage(chatId, text);
        }
    }

    /**
     * Попытка отправки AI-ответа с Markdown
     */
    private boolean trySendAiWithMarkdown(long chatId, String text) {
        try {
            // Очищаем лишнее экранирование от AI
            String cleaned = cleanAiText(text);
            String safeText = TelegramMarkdownEscapeUtil.escapeAllMarkdownChars(cleaned);

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(safeText);
            message.setParseMode("MarkdownV2");

            getBot().execute(message);
            log.debug("✅ AI response sent with MarkdownV2 to chat {}", chatId);
            return true;

        } catch (Exception e) {
            log.debug("AI Markdown failed for chat {}: {}", chatId, e.getMessage());
            return false;
        }
    }

    /**
     * Попытка отправки AI-ответа с HTML
     */
    private boolean trySendAiWithHtml(long chatId, String text) {
        try {
            String htmlText = TelegramMarkdownEscapeUtil.convertToHtml(text);

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(htmlText);
            message.setParseMode("HTML");

            getBot().execute(message);
            log.debug("✅ AI response sent with HTML to chat {}", chatId);
            return true;

        } catch (Exception e) {
            log.debug("AI HTML failed for chat {}: {}", chatId, e.getMessage());
            return false;
        }
    }

    /**
     * Очистка текста от AI от лишнего экранирования
     */
    private String cleanAiText(String text) {
        if (text == null) return "";

        // AI часто добавляет лишние экранирования
        // Убираем их, но сохраняем важное форматирование
        return text
                .replace("\\\\\\*", "\\*")
                .replace("\\\\\\_", "\\_")
                .replace("\\\\\\\\", "\\\\")
                .replace("\\\\`", "\\`")
                .replace("\\\\.", "\\.")
                .replace("\\\\!", "\\!");
    }

    /**
     * Специальный метод для отправки AI-ответов с fallback
     */
    private void sendAiResponseWithFallback(long chatId, String text) {
        try {
            String preparedText = TelegramMarkdownEscapeUtil.prepareAiResponse(text);

            SendMessage message = new SendMessage();
            message.setChatId(String.valueOf(chatId));
            message.setText(preparedText);
            message.setParseMode("MarkdownV2");

            getBot().execute(message);
            log.debug("✅ AI response sent with preparation to chat {}", chatId);

        } catch (Exception e) {
            log.warn("Prepared AI response failed, sending plain text: {}", e.getMessage());

            sendPlainText(chatId, text);
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


