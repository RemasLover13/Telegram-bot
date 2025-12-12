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

        String safeText = TelegramMarkdownEscapeUtil.escapeMarkdownSmart(text);

        if (safeText.length() <= SAFE_MESSAGE_LENGTH) {
            parts.add(safeText);
            return parts;
        }

        List<String> sections = splitBySections(safeText);

        if (sections.size() > 1) {
            for (String section : sections) {
                if (section.length() <= SAFE_MESSAGE_LENGTH) {
                    parts.add(section);
                } else {
                    parts.addAll(splitByParagraphs(section));
                }
            }
        } else {
            parts.addAll(splitByParagraphs(safeText));
        }

        return addNumberingToParts(parts);
    }

    /**
     * Разбивает текст по разделам (1., 2., и т.д.)
     */
    private List<String> splitBySections(String text) {
        List<String> sections = new ArrayList<>();

        Pattern sectionPattern = Pattern.compile("^(\\d+\\.\\s+.*?)(?=(?:^\\d+\\.\\s+|\\z))",
                Pattern.MULTILINE | Pattern.DOTALL);

        Matcher matcher = sectionPattern.matcher(text);
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
     * Разбивает на фиксированные чанки с учетом слов
     */
    public List<String> splitIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        String safeText = TelegramMarkdownEscapeUtil.escapeMarkdownSmart(text);

        int totalLength = safeText.length();
        int start = 0;
        int partNumber = 1;
        int totalParts = (int) Math.ceil((double) totalLength / chunkSize);

        while (start < totalLength) {
            int end = Math.min(start + chunkSize, totalLength);

            if (end < totalLength && !Character.isWhitespace(safeText.charAt(end))) {
                int lastSpace = safeText.lastIndexOf(' ', end);
                if (lastSpace > start + chunkSize / 2) {
                    end = lastSpace;
                }
            }

            String chunk = safeText.substring(start, end).trim();

            if (totalParts > 1) {
                chunk = String.format("📄 *Часть %d из %d:*\n\n%s",
                        partNumber, totalParts, chunk);

                if (partNumber < totalParts) {
                    chunk += String.format("\n\n_Продолжение... (%d/%d)_", partNumber, totalParts);
                }
            }

            chunks.add(chunk);
            start = end;

            if (start < totalLength && Character.isWhitespace(safeText.charAt(start))) {
                start++;
            }

            partNumber++;
        }

        return chunks;
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
            String header = String.format("📄 *Часть %d из %d:*\n\n", i + 1, parts.size());
            String footer = String.format("\n\n_Продолжение следует... (%d/%d)_", i + 1, parts.size());

            String numberedPart;
            if (i == parts.size() - 1) {
                numberedPart = header + parts.get(i);
            } else {
                numberedPart = header + parts.get(i) + footer;
            }

            numberedParts.add(numberedPart);
        }

        return numberedParts;
    }

    /**
     * Проверяет, нужно ли разбивать сообщение
     */
    public boolean needsSplitting(String text) {
        if (text == null) return false;
        return text.length() > SAFE_MESSAGE_LENGTH;
    }

    /**
     * Получает количество частей для текста
     */
    public int getEstimatedParts(String text) {
        if (text == null || text.isEmpty()) return 0;
        return (int) Math.ceil((double) text.length() / SAFE_MESSAGE_LENGTH);
    }
}
