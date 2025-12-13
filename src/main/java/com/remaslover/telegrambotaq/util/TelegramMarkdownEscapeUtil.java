package com.remaslover.telegrambotaq.util;

import org.springframework.stereotype.Component;

@Component
public class TelegramMarkdownEscapeUtil {


    private static final char[] MARKDOWN_V2_SPECIAL_CHARS = {
            '_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!'
    };

    /**
     * Полное экранирование для Telegram MarkdownV2
     * Экранирует ВСЕ специальные символы, включая ! и .
     */
    public static String escapeForTelegram(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        int i = 0;
        int length = text.length();

        while (i < length) {
            char currentChar = text.charAt(i);

            boolean inCodeBlock = isInsideCodeBlock(text, i);

            if (inCodeBlock) {
                result.append(currentChar);
                i++;
                continue;
            }

            if (currentChar == '`') {
                result.append(currentChar);
                i++;

                while (i < length && text.charAt(i) != '`') {
                    result.append(text.charAt(i));
                    i++;
                }

                if (i < length) {
                    result.append(text.charAt(i));
                    i++;
                }
                continue;
            }

            if (isMarkdownV2SpecialChar(currentChar)) {
                result.append('\\').append(currentChar);
            } else if (currentChar == '\\') {
                result.append("\\\\");
            } else if (currentChar == '&') {
                result.append("&amp;");
            } else if (currentChar == '<') {
                result.append("&lt;");
            } else if (currentChar == '>') {
                result.append("&gt;");
            } else {
                result.append(currentChar);
            }
            i++;
        }

        return result.toString();
    }

    /**
     * Проверяет, является ли символ специальным для MarkdownV2
     * Включая ! и .
     */
    private static boolean isMarkdownV2SpecialChar(char c) {
        return c == '_' || c == '*' || c == '[' || c == ']' ||
               c == '(' || c == ')' || c == '~' || c == '`' ||
               c == '>' || c == '#' || c == '+' || c == '-' ||
               c == '=' || c == '|' || c == '{' || c == '}' ||
               c == '.' || c == '!';
    }

    /**
     * Проверяет, находится ли позиция внутри блока кода ```
     */
    private static boolean isInsideCodeBlock(String text, int position) {
        int backtickCount = 0;
        boolean inCodeBlock = false;

        for (int i = 0; i < position; i++) {
            char c = text.charAt(i);

            if (c == '`') {
                backtickCount++;

                if (i + 2 < text.length() &&
                    text.charAt(i + 1) == '`' &&
                    text.charAt(i + 2) == '`') {
                    inCodeBlock = !inCodeBlock;
                    i += 2;
                    backtickCount += 2;
                }
            }
        }

        return inCodeBlock;
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
     * Альтернативный подход: отправка как HTML
     */
    public static String convertToHtml(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String html = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")

                .replace("**", "<b>")
                .replace("__", "<i>")
                .replace("\n", "<br>")

                // Блоки кода
                .replace("```", "<pre>")
                .replace("`", "<code>");

        return html;
    }


    /**
     * Упрощенное экранирование для обычного текста (без кода)
     */
    public static String escapePlainText(String text) {
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
                .replace("~", "\\~");
    }

    /**
     * Подготовка AI-ответа: очистка лишнего экранирования
     */
    public static String prepareAiResponse(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String cleaned = cleanAiResponse(text);

        return escapeForTelegram(cleaned);
    }

    /**
     * Очистка ответа от AI от лишнего экранирования
     */
    private static String cleanAiResponse(String text) {
        if (text == null) return "";


        StringBuilder result = new StringBuilder();
        int i = 0;
        int length = text.length();

        while (i < length) {
            char currentChar = text.charAt(i);

            if (currentChar == '\\' && i + 1 < length) {
                char nextChar = text.charAt(i + 1);

                if (nextChar == '\\' || nextChar == '`' || nextChar == '*') {
                    result.append(currentChar).append(nextChar);
                    i += 2;
                } else {
                    result.append(nextChar);
                    i += 2;
                }
            } else {
                result.append(currentChar);
                i++;
            }
        }

        return result.toString();
    }

    /**
     * Улучшенное экранирование для Telegram MarkdownV2
     * Экранирует ВСЕ специальные символы
     */
    public static String escapeMarkdownV2(String text) {
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
            } else if (c == '&') {
                result.append("&amp;");
            } else if (c == '<') {
                result.append("&lt;");
            } else if (c == '>') {
                result.append("&gt;");
            } else {
                result.append(c);
            }
        }

        return result.toString();
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
     * Экранирование для HTML с дополнительной безопасностью
     */
    public static String escapeHtml(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String escaped = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");

        escaped = escaped.replace("--", "&#45;&#45;");

        return escaped;
    }


    /**
     * Умное экранирование MarkdownV2:
     * - Не экранирует уже экранированные символы
     * - Сохраняет блоки кода
     * - Сохраняет существующее форматирование
     */
    public static String escapeMarkdownSmart(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        if (isAlreadyEscaped(text)) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        int i = 0;
        int length = text.length();

        while (i < length) {
            char currentChar = text.charAt(i);

            if (currentChar == '`' && i + 2 < length &&
                text.charAt(i + 1) == '`' && text.charAt(i + 2) == '`') {
                result.append("```");
                i += 3;

                while (i + 2 < length && !(text.charAt(i) == '`' &&
                                           text.charAt(i + 1) == '`' && text.charAt(i + 2) == '`')) {
                    result.append(text.charAt(i));
                    i++;
                }

                if (i + 2 < length) {
                    result.append("```");
                    i += 3;
                }
                continue;
            }

            if (currentChar == '`') {
                result.append('`');
                i++;

                while (i < length && text.charAt(i) != '`') {
                    result.append(text.charAt(i));
                    i++;
                }

                if (i < length) {
                    result.append('`');
                    i++;
                }
                continue;
            }

            if (currentChar == '\\' && i + 1 < length) {
                char nextChar = text.charAt(i + 1);

                if (isMarkdownSpecialChar(nextChar)) {
                    result.append('\\').append(nextChar);
                    i += 2;
                    continue;
                }
            }

            if (isMarkdownSpecialChar(currentChar)) {
                if (shouldEscapeChar(text, i)) {
                    result.append('\\');
                }
                result.append(currentChar);
                i++;
            } else {
                result.append(currentChar);
                i++;
            }
        }

        return result.toString();
    }

    /**
     * Проверяет, является ли символ специальным для Markdown
     */
    private static boolean isMarkdownSpecialChar(char c) {
        for (char special : MARKDOWN_V2_SPECIAL_CHARS) {
            if (c == special) {
                return true;
            }
        }
        return false;
    }

    /**
     * Проверяет, нужно ли экранировать символ в данной позиции
     */
    private static boolean shouldEscapeChar(String text, int position) {
        char currentChar = text.charAt(position);

        if (position > 0 && text.charAt(position - 1) == '\\') {
            int backslashCount = 0;
            int j = position - 1;
            while (j >= 0 && text.charAt(j) == '\\') {
                backslashCount++;
                j--;
            }

            if (backslashCount % 2 == 1) {
                return false;
            }
        }


        if (currentChar == '*') {
            if (position > 0 && position + 1 < text.length()) {
                char prev = text.charAt(position - 1);
                char next = text.charAt(position + 1);

                if (prev == '*' && next == '*') {
                    return false;
                }
            }
        }

        if (currentChar == '_') {
            if (position > 0 && position + 1 < text.length()) {
                char prev = text.charAt(position - 1);
                char next = text.charAt(position + 1);

                if (prev == '_' && next == '_') {
                    return false;
                }
            }
        }

        if (currentChar == '.' && (position == text.length() - 1 ||
                                   Character.isWhitespace(text.charAt(position + 1)))) {
            return false;
        }

        if (currentChar == '!' && (position == text.length() - 1 ||
                                   Character.isWhitespace(text.charAt(position + 1)))) {
            return false;
        }

        return true;
    }

    /**
     * Проверяет, уже ли правильно экранирован текст
     */
    private static boolean isAlreadyEscaped(String text) {
        int backtickCount = 0;
        boolean inCodeBlock = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '`') {
                backtickCount++;

                if (i + 2 < text.length() && text.charAt(i + 1) == '`' && text.charAt(i + 2) == '`') {
                    inCodeBlock = !inCodeBlock;
                    i += 2;
                    backtickCount += 2;
                }
            }
        }

        if (backtickCount % 2 != 0 && !inCodeBlock) {
            return false;
        }

        int bracketBalance = 0;
        int parenthesisBalance = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '[') bracketBalance++;
            else if (c == ']') bracketBalance--;
            else if (c == '(') parenthesisBalance++;
            else if (c == ')') parenthesisBalance--;

            if (bracketBalance < 0 || parenthesisBalance < 0) {
                return false;
            }
        }

        return bracketBalance == 0 && parenthesisBalance == 0;
    }

    /**
     * Минимальное экранирование только самых проблемных символов
     * Используется для текстов, где не нужно форматирование
     */
    public static String escapeMinimal(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        return text
                .replace("\\", "\\\\")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#");
    }

}
