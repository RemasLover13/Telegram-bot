package com.remaslover.telegrambotaq.util;

import org.springframework.stereotype.Component;

@Component
public class TelegramMarkdownEscapeUtil {
    /**
     * Экранирует все специальные символы Markdown для Telegram
     */
    public static String escapeMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder escaped = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '\\') {
                escaped.append("\\\\");
                continue;
            }

            if (isMarkdownCharacter(c)) {
                escaped.append('\\');
            }

            escaped.append(c);
        }

        return escaped.toString();
    }

    /**
     * Проверяет, является ли символ специальным для Markdown в Telegram
     */
    public static boolean isMarkdownCharacter(char c) {
        return c == '_' || c == '*' || c == '[' || c == ']' ||
               c == '(' || c == ')' || c == '~' || c == '`' ||
               c == '>' || c == '#' || c == '+' || c == '-' ||
               c == '=' || c == '|' || c == '{' || c == '}' ||
               c == '.' || c == '!';
    }

    /**
     * Расширенная проверка с учетом большего количества проблемных символов
     */
    public static boolean isProblematicCharacter(char c) {
        if (isMarkdownCharacter(c)) return true;

        return c == '&' || c == '<' || c == '>' || c == '"' || c == '\'' ||
               c == '@' || c == '^' || c == '%' || c == '$' || c == '\\';
    }

    /**
     * Полное экранирование для максимальной безопасности
     */
    public static String escapeAll(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder escaped = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            switch (c) {
                case '\\': escaped.append("\\\\"); break;
                case '_': escaped.append("\\_"); break;
                case '*': escaped.append("\\*"); break;
                case '[': escaped.append("\\["); break;
                case ']': escaped.append("\\]"); break;
                case '(': escaped.append("\\("); break;
                case ')': escaped.append("\\)"); break;

                case '~': escaped.append("\\~"); break;
                case '`': escaped.append("\\`"); break;
                case '#': escaped.append("\\#"); break;
                case '+': escaped.append("\\+"); break;
                case '-': escaped.append("\\-"); break;
                case '=': escaped.append("\\="); break;
                case '|': escaped.append("\\|"); break;
                case '{': escaped.append("\\{"); break;
                case '}': escaped.append("\\}"); break;
                case '.': escaped.append("\\."); break;
                case '!': escaped.append("\\!"); break;

                case '&': escaped.append("&amp;"); break;
                case '<': escaped.append("&lt;"); break;
                case '>': escaped.append("&gt;"); break;
                case '"': escaped.append("&quot;"); break;
                case '\'': escaped.append("&#39;"); break;

                default:
                    escaped.append(c);
            }
        }

        return escaped.toString();
    }

    /**
     * Безопасная отправка сообщения с автоматическим экранированием
     */
    public static String prepareMessage(String text, boolean useMarkdown) {
        if (!useMarkdown) {
            return text;
        }

        return escapeAll(text);
    }

    /**
     * Проверяет, содержит ли текст потенциально проблемные символы
     */
    public static boolean containsMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        for (char c : text.toCharArray()) {
            if (isProblematicCharacter(c)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Логирует проблемные символы для отладки
     */
    public static void logProblematicCharacters(String text, String prefix) {
        if (text == null) return;

        StringBuilder problematic = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (isProblematicCharacter(c)) {
                problematic.append(String.format(" [pos %d: '%c' (0x%04X)]", i, c, (int)c));
            }
        }

        if (problematic.length() > 0) {
            System.out.println(prefix + "Problematic chars: " + problematic.toString());
        }
    }
}
