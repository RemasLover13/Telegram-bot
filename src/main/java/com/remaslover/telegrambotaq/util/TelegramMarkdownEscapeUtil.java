package com.remaslover.telegrambotaq.util;

import org.springframework.stereotype.Component;

@Component
public class TelegramMarkdownEscapeUtil {


    // Все специальные символы для Telegram MarkdownV2
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

            // Проверяем, является ли символ специальным
            if (isMarkdownV2SpecialChar(c)) {
                // Проверяем, не экранирован ли уже
                if (i > 0 && text.charAt(i - 1) == '\\') {
                    int backslashCount = countConsecutiveBackslashes(text, i - 1);
                    // Если четное количество слешей - значит не экранирован
                    if (backslashCount % 2 == 0) {
                        result.append('\\').append(c);
                    } else {
                        // Уже экранирован
                        result.append(c);
                    }
                } else {
                    // Не экранирован - экранируем
                    result.append('\\').append(c);
                }
            } else if (c == '\\') {
                // Обработка обратного слеша
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

        // Экранируем ВСЕ специальные символы
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
                .replace("\\\\", "\\")      // Двойные слеши
                .replace("\\!", "!")        // Восклицательные знаки
                .replace("\\.", ".")        // Точки
                .replace("\\,", ",")        // Запятые
                .replace("\\:", ":")        // Двоеточия
                .replace("\\;", ";")        // Точки с запятой
                .replace("\\-", "-")        // Дефисы
                .replace("\\'", "'")        // Апострофы
                .replace("\\\"", "\"");     // Кавычки

        return cleaned;
    }


}
