package com.remaslover.telegrambotaq.util;

import org.springframework.stereotype.Component;

@Component
public class TelegramMarkdownEscapeUtil {


    private static final char[] MARKDOWN_V2_SPECIAL_CHARS = {
            '_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!'
    };

    /**
     * УЛУЧШЕННАЯ очистка - теперь удаляем ВСЁ экранирование от AI
     * Включая экранирование массивов \[ и \]
     */
    public static String cleanAiResponse(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String cleaned = text;

        cleaned = cleaned
                .replace("\\.", ".")
                .replace("\\(", "(")
                .replace("\\)", ")")
                .replace("\\,", ",")
                .replace("\\:", ":")
                .replace("\\;", ";")
                .replace("\\!", "!")
                .replace("\\?", "?")
                .replace("\\'", "'")
                .replace("\\\"", "\"")
                .replace("\\-", "-")
                .replace("\\=", "=")
                .replace("\\+", "+")
                .replace("\\|", "|")
                .replace("\\{", "{")
                .replace("\\}", "}")
                .replace("\\~", "~")
                // МАССИВЫ - очень важно!
                .replace("\\[", "[")
                .replace("\\]", "]");

        cleaned = cleaned.replace("\\\\\\\\", "\\\\");

        cleaned = cleaned
                .replace("\\\\*\\\\*", "**")
                .replace("\\\\`\\\\`\\\\`", "```")
                .replace("\\\\_\\\\_", "__")
                .replace("\\\\~\\\\~", "~~")
                .replace("\\\\`", "`")
                .replace("\\\\*", "*")
                .replace("\\\\_", "_");

        return cleaned;
    }

    /**
     * УПРОЩЕННАЯ и НАДЁЖНАЯ версия экранирования
     * Больше не пытаемся быть умными - просто экранируем ВСЁ,
     * кроме явных блоков кода и разметки
     */
    public static String escapeSmart(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String cleaned = cleanAiResponse(text);

        if (containsCodeBlocks(cleaned)) {
            return escapeWithCodeBlocks(cleaned);
        }

        return escapeAllMarkdownChars(cleaned);
    }

    /**
     * Определяет, есть ли в тексте блоки кода ```
     */
    private static boolean containsCodeBlocks(String text) {
        return text.contains("```");
    }

    /**
     * Обработка текста с блоками кода
     */
    private static String escapeWithCodeBlocks(String text) {
        StringBuilder result = new StringBuilder();

        String[] parts = text.split("```");

        for (int i = 0; i < parts.length; i++) {
            if (i % 2 == 0) {
                result.append(escapeAllMarkdownChars(parts[i]));
            } else {
                result.append("```").append(parts[i]).append("```");
            }
        }

        return result.toString();
    }

    /**
     * Экранирование ВСЕХ специальных символов
     * Просто, но надёжно
     */
    public static String escapeAllMarkdownChars(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\\') {
                result.append("\\\\");
            } else if (isMarkdownV2SpecialChar(c)) {
                result.append('\\').append(c);
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Проверяет, является ли символ специальным
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
     * Для совместимости
     */
    public static String escapeForTelegram(String text) {
        return escapeSmart(text);
    }

    /**
     * Фолбэк
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
                .replace("`", "\\`");
    }

}
