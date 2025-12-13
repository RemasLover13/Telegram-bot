package com.remaslover.telegrambotaq.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class MessageQueueService {

    private static final Logger log = LoggerFactory.getLogger(MessageQueueService.class);

    private final MessageSender messageSender;
    private final BlockingQueue<MessageTask> messageQueue = new LinkedBlockingQueue<>();

    public MessageQueueService(MessageSender messageSender) {
        this.messageSender = messageSender;
        startMessageProcessor();
    }

    /**
     * Ставит сообщение в очередь на отправку
     */
    public void enqueueMessage(long chatId, String text, int delayMs) {
        messageQueue.offer(new MessageTask(chatId, text, delayMs, false));
    }

    /**
     * Ставит AI-ответ в очередь на отправку (использует специальный метод)
     */
    public void enqueueAiResponse(long chatId, String text, int delayMs) {
        messageQueue.offer(new MessageTask(chatId, text, delayMs, true));
    }

    /**
     * Ставит несколько сообщений в очередь
     */
    public void enqueueMessages(long chatId, List<String> messages, int initialDelayMs) {
        int delay = initialDelayMs;
        for (String message : messages) {
            messageQueue.offer(new MessageTask(chatId, message, delay, false));
            delay += 1500;
        }
    }

    /**
     * Ставит несколько AI-ответов в очередь
     */
    public void enqueueAiResponses(long chatId, List<String> responses, int initialDelayMs) {
        int delay = initialDelayMs;
        for (String response : responses) {
            messageQueue.offer(new MessageTask(chatId, response, delay, true));
            delay += 1500;
        }
    }

    /**
     * Запускает обработчик очереди
     */
    private void startMessageProcessor() {
        new Thread(() -> {
            while (true) {
                try {
                    MessageTask task = messageQueue.take();

                    if (task.delayMs > 0) {
                        Thread.sleep(task.delayMs);
                    }

                    if (task.isAiResponse) {
                        messageSender.sendAiResponse(task.chatId, task.text);
                    } else {
                        messageSender.sendMessage(task.chatId, task.text);
                    }

                    log.debug("Sent queued message to chat {} (delay: {}ms, isAiResponse: {})",
                            task.chatId, task.delayMs, task.isAiResponse);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("Message queue processor interrupted");
                    break;
                } catch (Exception e) {
                    log.error("Error processing queued message: {}", e.getMessage(), e);
                }
            }
        }, "message-queue-processor").start();
    }

    /**
     * Задача отправки сообщения
     */
    private record MessageTask(long chatId, String text, int delayMs, boolean isAiResponse) {
    }
}
