package com.remaslover.telegrambotaq.util;

import org.springframework.stereotype.Component;

@Component
public class TelegramMarkdownEscapeUtil {
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
                    i += 2; // Пропускаем остальные два `
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

    /**
     * Альтернативный подход: разбить текст на части и экранировать отдельно
     */
    public static String escapeMarkdownPreserveCode(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        StringBuilder result = new StringBuilder();
        String[] parts = text.split("(?=```)|(?<=```)");

        boolean inCodeBlock = false;

        for (String part : parts) {
            if (part.equals("```")) {
                inCodeBlock = !inCodeBlock;
                result.append(part);
            } else if (inCodeBlock) {
                result.append(part);
            } else {
                result.append(escapeOutsideCodeBlocks(part));
            }
        }

        return result.toString();
    }

    /**
     * Экранирует текст вне блоков кода, но сохраняет inline код
     */
    private static String escapeOutsideCodeBlocks(String text) {
        StringBuilder result = new StringBuilder();
        boolean inInlineCode = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '`') {
                if (i + 2 < text.length() &&
                    text.charAt(i + 1) == '`' &&
                    text.charAt(i + 2) == '`') {
                    result.append("```");
                    i += 2;
                } else {
                    inInlineCode = !inInlineCode;
                    result.append(c);
                }
            } else if (inInlineCode) {
                result.append(c);
            } else {
                result.append(escapeBasicMarkdown(c));
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
