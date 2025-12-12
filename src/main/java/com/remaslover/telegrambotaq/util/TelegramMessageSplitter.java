package com.remaslover.telegrambotaq.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
     * Разбивает сообщение на части с сохранением форматирования
     */
    public List<String> splitMessageSmart(String text) {
        List<String> parts = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return parts;
        }

        try {
            String cleanText = TelegramMarkdownEscapeUtil.cleanDoubleEscaping(text);

            String safeText = TelegramMarkdownEscapeUtil.escapeMarkdownSmart(cleanText);

            log.debug("Text after escape: {} chars", safeText.length());

            if (safeText.length() <= SAFE_MESSAGE_LENGTH) {
                parts.add(safeText);
                return parts;
            }

            parts = splitByLogicalBlocks(safeText);

            if (parts.isEmpty() || (parts.size() == 1 && parts.get(0).length() > SAFE_MESSAGE_LENGTH)) {
                parts = splitByParagraphs(safeText);
            }

            parts = ensurePartsSize(parts);

            log.info("Split message into {} parts", parts.size());

            return parts;

        } catch (Exception e) {
            log.error("Error in splitMessageSmart: {}", e.getMessage(), e);

            return splitMessageSimple(text);
        }
    }

    /**
     * Разбивает текст по логическим блокам (заголовки, списки)
     */
    private List<String> splitByLogicalBlocks(String text) {
        List<String> blocks = new ArrayList<>();

        String[] sections = text.split("(?=^#{1,3}\\s)|(?=^\\*\\*\\*\\s*\\n)|(?=^---\\s*\\n)|(?=^\\d+\\.\\s)");

        StringBuilder currentBlock = new StringBuilder();

        for (String section : sections) {
            if (section.trim().isEmpty()) {
                continue;
            }

            if (currentBlock.length() + section.length() > SAFE_MESSAGE_LENGTH &&
                currentBlock.length() > MIN_PART_LENGTH) {
                blocks.add(currentBlock.toString().trim());
                currentBlock = new StringBuilder();
            }

            if (currentBlock.length() > 0) {
                currentBlock.append("\n\n");
            }
            currentBlock.append(section);
        }

        if (currentBlock.length() > 0) {
            blocks.add(currentBlock.toString().trim());
        }

        return blocks;
    }

    /**
     * Разбивает текст по параграфам
     */
    private List<String> splitByParagraphs(String text) {
        List<String> parts = new ArrayList<>();

        String[] paragraphs = text.split("\\n\\n");

        StringBuilder currentPart = new StringBuilder();

        for (String paragraph : paragraphs) {
            if (currentPart.length() + paragraph.length() + 2 > SAFE_MESSAGE_LENGTH &&
                currentPart.length() > MIN_PART_LENGTH) {

                parts.add(currentPart.toString().trim());
                currentPart = new StringBuilder();
            }

            if (currentPart.length() > 0) {
                currentPart.append("\n\n");
            }
            currentPart.append(paragraph);
        }

        if (currentPart.length() > 0) {
            parts.add(currentPart.toString().trim());
        }

        return parts;
    }

    /**
     * Проверяет размер частей и при необходимости разбивает дальше
     */
    private List<String> ensurePartsSize(List<String> parts) {
        List<String> result = new ArrayList<>();

        for (String part : parts) {
            if (part.length() <= SAFE_MESSAGE_LENGTH) {
                result.add(part);
            } else {
                result.addAll(splitIntoChunks(part, SAFE_MESSAGE_LENGTH));
            }
        }

        return result;
    }

    /**
     * Разбивает текст на чанки фиксированного размера
     */
    private List<String> splitIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            if (end < text.length() && !Character.isWhitespace(text.charAt(end))) {
                int lastSpace = text.lastIndexOf(' ', end);
                int lastNewline = text.lastIndexOf('\n', end);
                int breakPoint = Math.max(lastSpace, lastNewline);

                if (breakPoint > start + chunkSize / 2) {
                    end = breakPoint;
                }
            }

            chunks.add(text.substring(start, end).trim());
            start = end;

            while (start < text.length() && Character.isWhitespace(text.charAt(start))) {
                start++;
            }
        }

        return chunks;
    }

    /**
     * Простое разбиение без форматирования (fallback)
     */
    public List<String> splitMessageSimple(String text) {
        List<String> parts = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return parts;
        }

        String safeText = TelegramMarkdownEscapeUtil.escapeMinimal(text);

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
}
