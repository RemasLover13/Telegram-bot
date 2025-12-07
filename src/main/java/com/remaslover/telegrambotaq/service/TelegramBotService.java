package com.remaslover.telegrambotaq.service;


import com.remaslover.telegrambotaq.config.TelegramBotConfig;
import com.remaslover.telegrambotaq.enums.Button;
import com.vdurmont.emoji.EmojiParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

@Service
public class TelegramBotService extends TelegramLongPollingBot {

    private final TelegramBotConfig config;
    private final UserService userService;
    @Lazy
    private final CommandHandler commandHandler;
    private final KeyboardManager keyboardManager;
    private final MessageSender messageSender;

    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);

    public TelegramBotService(TelegramBotConfig config,
                              UserService userService,
                              CommandHandler commandHandler,
                              KeyboardManager keyboardManager,
                              MessageSender messageSender) {
        super(config.getBotToken());
        this.config = config;
        this.userService = userService;
        this.commandHandler = commandHandler;
        this.keyboardManager = keyboardManager;
        this.messageSender = messageSender;

        initializeBotCommands();
    }

    private void initializeBotCommands() {
        List<BotCommand> commands = keyboardManager.createBotCommands();
        try {
            this.execute(new SetMyCommands(commands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot commands: {}", e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getBotToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            Long userId = update.getMessage().getFrom().getId();

            log.info("ChatId: {}, UserId: {}, Message: {}", chatId, userId, messageText);

            userService.registerUser(update.getMessage());

            if (messageText.contains("/send") && config.getBotOwner().equals(chatId)) {
                handleBroadcastMessage(messageText);
            } else if (messageText.startsWith("/ai")) {
                commandHandler.handleAiRequest(chatId, userId, messageText);
            } else if (messageText.startsWith("/topnews")) {
                commandHandler.handleTopNewsCommand(chatId, messageText);
            } else if (messageText.startsWith("/news_category")) {
                commandHandler.handleNewsCategoryCommand(chatId, messageText);
            } else if (messageText.startsWith("/news_country")) {
                commandHandler.handleNewsCountryCommand(chatId, messageText);
            } else if (messageText.startsWith("/news_search")) {
                commandHandler.handleNewsSearchCommand(chatId, messageText);
            } else if (messageText.equals("/credits")) {
                commandHandler.handleCreditsCommand(chatId);
            } else {
                commandHandler.handleRegularCommands(chatId, userId, messageText, update.getMessage());
            }
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        }
    }

    private void handleBroadcastMessage(String messageText) {
        var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
        var users = userService.getAllUsers();
        for (var user : users) {
            prepareAndSendMessage(user.getId(), textToSend);
        }
        log.info("Broadcast message sent to {} users", users.size());
    }

    private void handleCallbackQuery(Update update) {
        String callbackQuery = update.getCallbackQuery().getData();
        long messageId = update.getCallbackQuery().getMessage().getMessageId();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (callbackQuery.equals(Button.YES_BUTTON.name())) {
            String text = "✅ Вы нажали кнопку ДА - регистрация выполнена!";
            messageSender.editMessage(chatId, (int) messageId, text);
        } else if (callbackQuery.equals(Button.NO_BUTTON.name())) {
            String text = "❌ Вы нажали кнопку НЕТ - регистрация отменена.";
            messageSender.editMessage(chatId, (int) messageId, text);
        }
    }


    public void prepareAndSendMessage(long chatId, String textToSend) {
        messageSender.sendMessage(chatId, textToSend);
    }

    public void sendMessageWithKeyboard(long chatId, String textToSend) {
        ReplyKeyboardMarkup keyboard = keyboardManager.createMainKeyboard();
        messageSender.sendMessageWithKeyboard(chatId, textToSend, keyboard);
    }

    public void register(long chatId) {
        String messageText = "Вы хотите зарегистрироваться в системе?";
        InlineKeyboardMarkup keyboard = keyboardManager.createRegistrationKeyboard();
        messageSender.sendMessageWithInlineKeyboard(chatId, messageText, keyboard);
    }

}