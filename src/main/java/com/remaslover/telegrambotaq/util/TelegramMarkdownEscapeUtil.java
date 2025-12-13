package com.remaslover.telegrambotaq.util;

import org.springframework.stereotype.Component;

@Component
public class TelegramMarkdownEscapeUtil {


    private static final char[] MARKDOWN_V2_SPECIAL_CHARS = {
            '_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!'
    };

    /**
     * Полное экранирование для Telegram MarkdownV2
     * Экранирует ВСЕ специальные символы
     */
    public static String escapeForTelegram(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (isMarkdownV2SpecialChar(c)) {
                if (i > 0 && text.charAt(i - 1) == '\\') {
                    int backslashCount = countConsecutiveBackslashes(text, i - 1);
                    if (backslashCount % 2 == 0) {
                        result.append('\\').append(c);
                    } else {
                        result.append(c);
                    }
                } else {
                    result.append('\\').append(c);
                }
            } else if (c == '\\') {
                result.append("\\\\");
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Проверяет, является ли символ специальным для MarkdownV2
     */
    private static boolean isMarkdownV2SpecialChar(char c) {
        for (char special : MARKDOWN_V2_SPECIAL_CHARS) {
            if (c == special) {
                return true;
            }
        }
        return false;
    }

    /**
     * Считает количество последовательных обратных слешей
     */
    private static int countConsecutiveBackslashes(String text, int position) {
        int count = 0;
        while (position >= 0 && text.charAt(position) == '\\') {
            count++;
            position--;
        }
        return count;
    }

    /**
     * Упрощенная версия для гарантированной работы
     */
    public static String escapeAllMarkdownChars(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return text
                .replace("\\", "\\\\")
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }

    /**
     * Очистка AI-ответов от лишнего экранирования
     */
    public static String cleanAiResponse(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String cleaned = text
                .replace("\\\\", "\\")
                .replace("\\!", "!")
                .replace("\\.", ".")
                .replace("\\,", ",")
                .replace("\\:", ":")
                .replace("\\;", ";")
                .replace("\\-", "-")
                .replace("\\'", "'")
                .replace("\\\"", "\"");

        return cleaned;
    }


    /**
     * Минимальное экранирование только самых проблемных символов
     */
    public static String escapeMinimal(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return text
                .replace("\\", "\\\\")
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#");
    }

    /**
     * Умное экранирование MarkdownV2 (сохраняет форматирование)
     */
    public static String escapeMarkdownSmart(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return escapeForTelegram(text);
    }

    /**
     * Экранирование для MarkdownV2 (аналогично escapeForTelegram)
     */
    public static String escapeMarkdownV2(String text) {
        return escapeAllMarkdownChars(text);
    }


}
