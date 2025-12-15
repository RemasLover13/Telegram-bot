package com.remaslover.telegrambotaq.util;

import org.springframework.stereotype.Component;

@Component
public class TelegramMarkdownEscapeUtil {


    private static final char[] MARKDOWN_V2_SPECIAL_CHARS = {
            '_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!'
    };

    /**
     * АГРЕССИВНАЯ очистка ответов AI - удаляем ВСЁ лишнее экранирование
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
                .replace("\\~", "~");

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
     * Умное экранирование - экранируем только отдельные спецсимволы
     */
    public static String escapeSmart(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String cleaned = cleanAiResponse(text);

        StringBuilder result = new StringBuilder();
        char[] chars = cleaned.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if (c == '\\') {
                result.append("\\\\");
            } else if (isMarkdownV2SpecialChar(c)) {
                if (isValidMarkdownPattern(chars, i)) {
                    result.append(c);
                } else {
                    result.append('\\').append(c);
                }
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Проверяет, является ли символ частью ВАЛИДНОЙ разметки Telegram
     */
    private static boolean isValidMarkdownPattern(char[] chars, int pos) {
        char c = chars[pos];

        if (c == '*') {
            if (pos > 0 && chars[pos - 1] == '*') return true;
            if (pos < chars.length - 1 && chars[pos + 1] == '*') return true;
        }

        if (c == '`') {
            if (pos > 0 && chars[pos - 1] == '`') return true;
            if (pos < chars.length - 1 && chars[pos + 1] == '`') return true;
        }

        if (c == '_') {
            if (pos > 0 && chars[pos - 1] == '_') return true;
            if (pos < chars.length - 1 && chars[pos + 1] == '_') return true;
        }

        if (c == '~') {
            if (pos > 0 && chars[pos - 1] == '~') return true;
            if (pos < chars.length - 1 && chars[pos + 1] == '~') return true;
        }

        if (c == '[') {
            for (int i = pos + 1; i < chars.length; i++) {
                if (chars[i] == ']' && i + 1 < chars.length && chars[i + 1] == '(') {
                    return true;
                }
                if (chars[i] == '\n') break;
            }
        }

        if (c == ']' && pos > 0 && chars[pos - 1] != '\\') {
            for (int i = pos - 1; i >= 0; i--) {
                if (chars[i] == '[') return true;
                if (chars[i] == '\n') break;
            }
        }

        if (c == '(') {
            for (int i = pos - 1; i >= 0; i--) {
                if (chars[i] == ']') return true;
                if (chars[i] == '\n') break;
            }
        }

        if (c == ')') {
            for (int i = pos - 1; i >= 0; i--) {
                if (chars[i] == '(') {
                    for (int j = i - 1; j >= 0; j--) {
                        if (chars[j] == ']') return true;
                        if (chars[j] == '\n') break;
                    }
                }
                if (chars[i] == '\n') break;
            }
        }

        return false;
    }

    /**
     * Простая версия для совместимости
     */
    public static String escapeForTelegram(String text) {
        return escapeSmart(text);
    }

    /**
     * Фолбэк на случай крайней необходимости
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
                .replace(")", "\\)");
    }

    private static boolean isMarkdownV2SpecialChar(char c) {
        for (char special : MARKDOWN_V2_SPECIAL_CHARS) {
            if (c == special) {
                return true;
            }
        }
        return false;
    }

}
