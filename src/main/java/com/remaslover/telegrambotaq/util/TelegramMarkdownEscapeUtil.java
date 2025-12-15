package com.remaslover.telegrambotaq.util;

import org.springframework.stereotype.Component;

@Component
public class TelegramMarkdownEscapeUtil {


    private static final char[] MARKDOWN_V2_SPECIAL_CHARS = {
            '_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!'
    };

    /**
     * УМНОЕ экранирование для Telegram MarkdownV2
     * Сохраняет разметку (жирный, курсив, код), экранирует только проблемные символы
     */
    public static String escapeSmart(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String cleaned = cleanAiResponse(text);

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < cleaned.length(); i++) {
            char c = cleaned.charAt(i);

            if (c == '\\') {
                result.append("\\\\");
            } else if (isMarkdownV2SpecialChar(c)) {
                if (isPartOfMarkup(cleaned, i)) {
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
     * Проверяет, является ли символ частью разметки
     */
    private static boolean isPartOfMarkup(String text, int position) {
        if (position < 0 || position >= text.length()) {
            return false;
        }

        char c = text.charAt(position);
        String surrounding = getSurroundingChars(text, position, 3);

        switch (c) {
            case '*':
                return surrounding.contains("**") ||
                       (position > 0 && text.charAt(position - 1) == '*') ||
                       (position < text.length() - 1 && text.charAt(position + 1) == '*');

            case '`':
                return surrounding.contains("```") ||
                       (position > 0 && text.charAt(position - 1) == '`') ||
                       (position < text.length() - 1 && text.charAt(position + 1) == '`');

            case '_':
                return surrounding.contains("__") ||
                       (position > 0 && text.charAt(position - 1) == '_') ||
                       (position < text.length() - 1 && text.charAt(position + 1) == '_');

            case '[':
                return position < text.length() - 1 && text.charAt(position + 1) != ' ';

            case ']':
                return position > 0 && text.charAt(position - 1) != ' ';

            case '(':
            case ')':
                return false;

            case '~':
                return surrounding.contains("~~");

            case '#':
            case '>':
                return position == 0 || text.charAt(position - 1) == '\n';

            default:
                return false;
        }
    }

    /**
     * Получает окружающие символы для анализа контекста
     */
    private static String getSurroundingChars(String text, int position, int radius) {
        int start = Math.max(0, position - radius);
        int end = Math.min(text.length(), position + radius + 1);
        return text.substring(start, end);
    }

    /**
     * Улучшенная очистка AI-ответов
     */
    public static String cleanAiResponse(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String step1 = text
                .replace("\\\\n", "\n")
                .replace("\\\\t", "\t")
                .replace("\\\\\"", "\"")
                .replace("\\\\'", "'");

        String step2 = step1
                .replace("\\*\\*", "**")
                .replace("\\*", "*")
                .replace("\\`\\`\\`", "```")
                .replace("\\`", "`")
                .replace("\\_\\_", "__")
                .replace("\\_", "_")
                .replace("\\~\\~", "~~");

        return step2;
    }

    /**
     * Для совместимости с существующим кодом
     */
    public static String escapeForTelegram(String text) {
        return escapeSmart(text);
    }

    /**
     * Минимальное экранирование (для fallback)
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

}
