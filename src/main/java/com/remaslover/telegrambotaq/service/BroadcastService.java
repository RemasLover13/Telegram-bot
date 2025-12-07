package com.remaslover.telegrambotaq.service;

import com.vdurmont.emoji.EmojiParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BroadcastService {
    private static final Logger log = LoggerFactory.getLogger(BroadcastService.class);
    private final UserService userService;
    private final MessageSender messageSender;

    public BroadcastService(UserService userService, MessageSender messageSender) {
        this.userService = userService;
        this.messageSender = messageSender;
    }

    public void broadcastMessage(String messageText) {
        var textToSend = EmojiParser.parseToUnicode(messageText);
        var users = userService.getAllUsers();

        for (var user : users) {
            messageSender.sendMessage(user.getId(), textToSend);
        }

        log.info("Broadcast message sent to {} users", users.size());
    }
}
