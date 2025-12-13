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
     * Основной метод для разбиения AI-ответов
     */
    public List<String> splitMessage(String text) {
        List<String> parts = new ArrayList<>();

        if (text == null || text.isEmpty()) {
            return parts;
        }

        try {
            String cleanedText = TelegramMarkdownEscapeUtil.cleanAiResponse(text);

            String safeText = TelegramMarkdownEscapeUtil.escapeForTelegram(cleanedText);

            if (safeText.length() <= SAFE_MESSAGE_LENGTH) {
                parts.add(safeText);
                return parts;
            }

            parts = splitPreservingCodeBlocks(safeText);

            log.info("Split message into {} parts", parts.size());

            return parts;

        } catch (Exception e) {
            log.error("Error splitting message: {}", e.getMessage(), e);

            return List.of(TelegramMarkdownEscapeUtil.escapeMinimal(text));
        }
    }

    /**
     * Разбивает текст, сохраняя блоки кода
     */
    private List<String> splitPreservingCodeBlocks(String text) {
        List<String> parts = new ArrayList<>();

        List<CodeBlockInfo> codeBlocks = findCodeBlocks(text);

        if (codeBlocks.isEmpty()) {
            return splitByParagraphs(text);
        }

        int currentPos = 0;
        StringBuilder currentPart = new StringBuilder();

        for (CodeBlockInfo block : codeBlocks) {
            String beforeCode = text.substring(currentPos, block.start);

            if (currentPart.length() + beforeCode.length() > SAFE_MESSAGE_LENGTH &&
                currentPart.length() > MIN_PART_LENGTH) {
                parts.add(currentPart.toString());
                currentPart = new StringBuilder();
            }

            if (currentPart.length() > 0 && !beforeCode.isEmpty()) {
                currentPart.append("\n\n");
            }
            currentPart.append(beforeCode);

            if (currentPart.length() + block.content.length() > SAFE_MESSAGE_LENGTH &&
                currentPart.length() > MIN_PART_LENGTH) {
                parts.add(currentPart.toString());
                currentPart = new StringBuilder();
            }

            if (currentPart.length() > 0) {
                currentPart.append("\n\n");
            }
            currentPart.append(block.content);

            currentPos = block.end;
        }

        if (currentPos < text.length()) {
            String remaining = text.substring(currentPos);
            if (currentPart.length() + remaining.length() > SAFE_MESSAGE_LENGTH &&
                currentPart.length() > MIN_PART_LENGTH) {
                parts.add(currentPart.toString());
                currentPart = new StringBuilder();
            }

            if (currentPart.length() > 0 && !remaining.isEmpty()) {
                currentPart.append("\n\n");
            }
            currentPart.append(remaining);
        }

        if (currentPart.length() > 0) {
            parts.add(currentPart.toString());
        }

        return parts;
    }

    /**
     * Находит блоки кода в тексте
     */
    private List<CodeBlockInfo> findCodeBlocks(String text) {
        List<CodeBlockInfo> blocks = new ArrayList<>();
        Pattern pattern = Pattern.compile("```[\\s\\S]*?```");
        Matcher matcher = pattern.matcher(text);

        while (matcher.find()) {
            blocks.add(new CodeBlockInfo(matcher.group(), matcher.start(), matcher.end()));
        }

        return blocks;
    }

    /**
     * Разбивает по параграфам
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
     * Вспомогательный класс для информации о блоке кода
     */
    private static class CodeBlockInfo {
        String content;
        int start;
        int end;

        CodeBlockInfo(String content, int start, int end) {
            this.content = content;
            this.start = start;
            this.end = end;
        }
    }
}
