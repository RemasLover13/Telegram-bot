package com.remaslover.telegrambotaq.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Утилита для разбивки длинных сообщений для Telegram
 */
@Component
public class TelegramMessageSplitter {
    private static final Logger log = LoggerFactory.getLogger(TelegramMessageSplitter.class);

    private static final int MAX_MESSAGE_LENGTH = 4096;
    private static final int SAFE_MESSAGE_LENGTH = 3500;
    private static final int MIN_PART_LENGTH = 100;

    /**
     * Умное разбиение сообщения на части
     */
    public List<String> splitMessageSmart(String text) {
        List<String> parts = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return parts;
        }

        try {
            String safeText = escapeMarkdownV2(text);

            if (safeText.length() <= SAFE_MESSAGE_LENGTH) {
                parts.add(safeText);
                return parts;
            }

            parts = splitIntoSections(safeText);

            if (parts.isEmpty() || (parts.size() == 1 && parts.get(0).length() > SAFE_MESSAGE_LENGTH)) {
                parts = splitByParagraphs(safeText);
            }

            if (parts.size() > 1) {
                parts = addNumberingToParts(parts);
            }

            return parts;

        } catch (Exception e) {
            log.error("Error in splitMessageSmart: {}", e.getMessage());

            return splitMessageSimple(text);
        }
    }

    /**
     * Экранирование Markdown V2 для Telegram
     */
    private String escapeMarkdownV2(String text) {
        if (text == null) return "";

        Pattern codeBlockPattern = Pattern.compile("```(.*?)```", Pattern.DOTALL);
        Matcher matcher = codeBlockPattern.matcher(text);

        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            result.append(escapeMarkdownBasic(text.substring(lastEnd, matcher.start())));

            result.append(matcher.group(0));

            lastEnd = matcher.end();
        }

        if (lastEnd < text.length()) {
            result.append(escapeMarkdownBasic(text.substring(lastEnd)));
        }

        return result.toString();
    }

    /**
     * Базовое экранирование Markdown символов
     */
    private String escapeMarkdownBasic(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        char[] specialChars = {'_', '*', '[', ']', '(', ')', '~', '`', '>', '#', '+', '-', '=', '|', '{', '}', '.', '!'};

        StringBuilder result = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            boolean isSpecial = false;

            for (char special : specialChars) {
                if (c == special) {
                    isSpecial = true;
                    break;
                }
            }

            if (isSpecial) {
                result.append('\\');
            }

            result.append(c);
        }

        return result.toString();
    }

    /**
     * Разбивает текст на логические секции
     */
    private List<String> splitIntoSections(String text) {
        List<String> sections = new ArrayList<>();

        Pattern pattern = Pattern.compile("^(\\d+\\.\\s+.*?)(?=(?:^\\d+\\.\\s+|^##\\s+|^#\\s+|^\\*\\*\\*|\\z))",
                Pattern.MULTILINE | Pattern.DOTALL);

        Matcher matcher = pattern.matcher(text);
        int lastEnd = 0;

        while (matcher.find()) {
            if (matcher.start() > lastEnd) {
                String prefix = text.substring(lastEnd, matcher.start()).trim();
                if (!prefix.isEmpty()) {
                    sections.add(prefix);
                }
            }

            sections.add(matcher.group(1).trim());
            lastEnd = matcher.end();
        }

        if (lastEnd < text.length()) {
            String suffix = text.substring(lastEnd).trim();
            if (!suffix.isEmpty()) {
                sections.add(suffix);
            }
        }

        return sections;
    }

    /**
     * Разбивает текст по параграфам
     */
    private List<String> splitByParagraphs(String text) {
        List<String> parts = new ArrayList<>();
        String[] paragraphs = text.split("\n\n");

        StringBuilder currentPart = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (currentPart.length() + paragraph.length() + 2 > SAFE_MESSAGE_LENGTH &&
                currentPart.length() > MIN_PART_LENGTH) {

                parts.add(currentPart.toString().trim());
                currentPart = new StringBuilder();
            }

            if (!currentPart.isEmpty()) {
                currentPart.append("\n\n");
            }
            currentPart.append(paragraph);
        }

        if (!currentPart.isEmpty()) {
            parts.add(currentPart.toString().trim());
        }

        return parts;
    }

    /**
     * Простое разбиение без форматирования (fallback)
     */
    public List<String> splitMessageSimple(String text) {
        List<String> parts = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return parts;
        }

        String safeText = text
                .replace("\\", "\\\\")
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("`", "\\`")
                .replace("[", "\\[")
                .replace("]", "\\]");

        if (safeText.length() <= SAFE_MESSAGE_LENGTH) {
            parts.add(safeText);
            return parts;
        }

        int chunkSize = SAFE_MESSAGE_LENGTH;
        int totalLength = safeText.length();

        for (int i = 0; i < totalLength; i += chunkSize) {
            int end = Math.min(i + chunkSize, totalLength);

            if (end < totalLength && !Character.isWhitespace(safeText.charAt(end))) {
                int lastSpace = safeText.lastIndexOf(' ', end);
                if (lastSpace > i + chunkSize / 2) {
                    end = lastSpace;
                }
            }

            parts.add(safeText.substring(i, end).trim());
        }

        return parts;
    }

    /**
     * Добавляет нумерацию к частям
     */
    private List<String> addNumberingToParts(List<String> parts) {
        if (parts.size() <= 1) {
            return parts;
        }

        List<String> numberedParts = new ArrayList<>();

        for (int i = 0; i < parts.size(); i++) {
            String part = parts.get(i);

            if (i < parts.size() - 1) {
                part += String.format("\n\n📄 *Продолжение... (%d/%d)*", i + 1, parts.size());
            }

            numberedParts.add(part);
        }

        return numberedParts;
    }

}
