package com.remaslover.telegrambotaq.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TelegramMarkdownEscapeUtil {

    private static final Logger log = LoggerFactory.getLogger(TelegramMarkdownEscapeUtil.class);

    /**
     * Проверяет, безопасна ли строка для отправки в Telegram
     */
    public static boolean isMarkdownSafe(String text) {
        if (text == null) return true;

        try {
            int backtickCount = 0;
            int starCount = 0;
            int underscoreCount = 0;

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);

                if (c == '`') backtickCount++;
                if (c == '*') starCount++;
                if (c == '_') underscoreCount++;

                if (i >= 2 &&
                    text.charAt(i - 2) == '`' &&
                    text.charAt(i - 1) == '`' &&
                    text.charAt(i) == '`') {
                    i += 2;
                }
            }

            if (backtickCount % 3 != 0) {
                log.warn("Unbalanced backticks: {}", backtickCount);
                return false;
            }

            if (text.contains("\\`") && !text.contains("`")) {
                return false;
            }

            return true;

        } catch (Exception e) {
            log.error("Error checking markdown safety: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Упрощенное экранирование для гарантированной безопасности
     */
    public static String escapeForTelegram(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            switch (c) {
                case '\\':
                    result.append("\\\\");
                    break;
                case '_':
                    result.append("\\_");
                    break;
                case '*':
                    result.append("\\*");
                    break;
                case '[':
                    result.append("\\[");
                    break;
                case ']':
                    result.append("\\]");
                    break;
                case '(':
                    result.append("\\(");
                    break;
                case ')':
                    result.append("\\)");
                    break;
                case '~':
                    result.append("\\~");
                    break;
                case '`':
                    result.append("\\`");
                    break;
                case '>':
                    result.append("\\>");
                    break;
                case '#':
                    result.append("\\#");
                    break;
                case '+':
                    result.append("\\+");
                    break;
                case '-':
                    result.append("\\-");
                    break;
                case '=':
                    result.append("\\=");
                    break;
                case '|':
                    result.append("\\|");
                    break;
                case '{':
                    result.append("\\{");
                    break;
                case '}':
                    result.append("\\}");
                    break;
                case '.':
                    result.append("\\.");
                    break;
                case '!':
                    result.append("\\!");
                    break;
                default:
                    result.append(c);
            }
        }

        return result.toString();
    }

    /**
     * Умное экранирование: сохраняет форматирование кода, экранирует остальное
     */
    public static String escapeMarkdownSmart(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        boolean inCodeBlock = false;
        boolean inInlineCode = false;
        int backtickCount = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '`') {
                backtickCount++;
                if (backtickCount == 3) {
                    inCodeBlock = !inCodeBlock;
                    backtickCount = 0;
                    result.append("```");
                    i += 2;
                    continue;
                }
            } else {
                backtickCount = 0;
            }

            if (c == '`' && !inCodeBlock) {
                inInlineCode = !inInlineCode;
                result.append(c);
                continue;
            }

            if (inCodeBlock || inInlineCode) {
                result.append(c);
                continue;
            }

            if (c == '\\') {
                result.append("\\\\");
            } else if (c == '_' || c == '*' || c == '[' || c == ']' ||
                       c == '(' || c == ')' || c == '~') {
                result.append('\\').append(c);
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

    private static String escapeBasicMarkdown(char c) {
        switch (c) {
            case '\\':
                return "\\\\";
            case '_':
                return "\\_";
            case '*':
                return "\\*";
            case '[':
                return "\\[";
            case ']':
                return "\\]";
            case '(':
                return "\\(";
            case ')':
                return "\\)";
            case '~':
                return "\\~";
            case '`':
                return "\\`";
            case '>':
                return "\\>";
            case '#':
                return "\\#";
            case '+':
                return "\\+";
            case '-':
                return "\\-";
            case '=':
                return "\\=";
            case '|':
                return "\\|";
            case '{':
                return "\\{";
            case '}':
                return "\\}";
            case '&':
                return "&amp;";
            case '<':
                return "&lt;";
            default:
                return String.valueOf(c);
        }
    }

    /**
     * Упрощенное экранирование только самых проблемных символов
     * (оставляет * для жирного текста и _ для курсива)
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
}
