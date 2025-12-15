package com.remaslover.telegrambotaq.util;

import org.springframework.stereotype.Component;

@Component
public class TelegramMarkdownEscapeUtil {


    /**
     * Упрощенная версия для гарантированной работы
     * ПРАВИЛЬНОЕ экранирование для Telegram MarkdownV2
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
     * Умное экранирование (лучше читается)
     * Экранирует только то, что нужно
     */
    public static String escapeSmart(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder(text.length() + 50);

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            switch (c) {
                case '\\':
                    result.append("\\\\");
                    break;
                case '_':
                case '*':
                case '[':
                case ']':
                case '(':
                case ')':
                case '~':
                case '`':
                case '>':
                case '#':
                case '+':
                case '-':
                case '=':
                case '|':
                case '{':
                case '}':
                case '.':
                case '!':
                    result.append('\\').append(c);
                    break;
                default:
                    result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Минимальное экранирование (самые проблемные символы)
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
                .replace("`", "\\`")
                .replace("#", "\\#");
    }

    /**
     * Очистка AI-ответов от лишнего экранирования
     * УБИРАЕМ лишние экранирования, которые мог добавить AI
     */
    public static String cleanAiResponse(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return text
                .replace("\\\\\\\\", "\\\\")

                .replace("\\!", "!")
                .replace("\\.", ".")
                .replace("\\,", ",")
                .replace("\\:", ":")
                .replace("\\;", ";")
                .replace("\\-", "-")
                .replace("\\'", "'")
                .replace("\\\"", "\"")
                .replace("\\?", "?");
    }

    /**
     * Полное экранирование для Telegram MarkdownV2
     * (для совместимости, вызывает escapeAllMarkdownChars)
     */
    public static String escapeForTelegram(String text) {
        return escapeAllMarkdownChars(text);
    }

}
