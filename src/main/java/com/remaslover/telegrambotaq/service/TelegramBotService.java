package com.remaslover.telegrambotaq.service;


import com.remaslover.telegrambotaq.config.TelegramBotConfig;
import com.remaslover.telegrambotaq.entity.Button;
import com.remaslover.telegrambotaq.entity.Joke;
import com.remaslover.telegrambotaq.entity.User;
import com.remaslover.telegrambotaq.repository.AdvertisementRepository;
import com.remaslover.telegrambotaq.repository.JokeRepository;
import com.remaslover.telegrambotaq.repository.UserRepository;
import com.remaslover.telegrambotaq.util.JokesParser;
import com.vdurmont.emoji.EmojiParser;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional(readOnly = true)
public class TelegramBotService extends TelegramLongPollingBot {

    private final TelegramBotConfig config;
    private final UserRepository userRepository;
    private final AdvertisementRepository advertisementRepository;
    private final JokeRepository jokeRepository;
    public static final String HELP_TEXT = "This bot generates answers to questions";
    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);

    public TelegramBotService(TelegramBotConfig config, UserRepository userRepository, AdvertisementRepository advertisementRepository, JokeRepository jokeRepository) {
        this.config = config;
        this.userRepository = userRepository;
        this.advertisementRepository = advertisementRepository;
        this.jokeRepository = jokeRepository;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get a welcome message"));
        listOfCommands.add(new BotCommand("/settings", "set your settings"));
        listOfCommands.add(new BotCommand("/help", "get a help message"));
        listOfCommands.add(new BotCommand("/my_data", "get your data"));
        listOfCommands.add(new BotCommand("/delete_data", "delete your data"));
        listOfCommands.add(new BotCommand("/time", "It shows the current time"));
        listOfCommands.add(new BotCommand("/joke", "random joke"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }


    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    public String getBotToken() {
        return config.getBotToken();
    }


    @Override
    public void onRegister() {
        super.onRegister();
    }

    @Override
    public void onUpdateReceived(Update update) {

        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            System.out.println("ChatId: " + chatId);
            System.out.println("OwnerId: " + config.getBotOwner());
            if (messageText.contains("/send") && config.getBotOwner().equals(chatId)) {
                var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
                var users = userRepository.findAll();

                for (var user : users) {
                    prepareAndSendMessage(user.getId(), textToSend);
                }

            } else {
                switch (messageText) {
                    case "/start":
                        registerUser(update.getMessage());
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                        break;
                    case "/help":
                        prepareAndSendMessage(chatId, HELP_TEXT);
                        break;
                    case "/my_data":
                        User user = getUser(update.getMessage());
                        if (user != null) {
                            prepareAndSendMessage(chatId, user.toString());
                        } else {
                            prepareAndSendMessage(chatId, "No user found");
                        }
                        break;
                    case "/delete_data":
                        boolean isSuccess = deleteUser(update.getMessage());
                        if (isSuccess) {
                            prepareAndSendMessage(chatId, "Successfully deleted data");
                        } else {
                            prepareAndSendMessage(chatId, "Failed to delete data");
                        }
                        break;
                    case "/time":
                        LocalDateTime localDateTime = LocalDateTime.now();
                        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                        String formattedTime = localDateTime.format(formatter);
                        prepareAndSendMessage(chatId, "Current time is " + formattedTime + " \uD83D\uDD5B");
                        break;
                    case "/register":
                        register(chatId);
                        break;
                    case "/joke":
                        getRandomJoke(chatId);
                        break;
                    default:
                        prepareAndSendMessage(chatId, "Something went wrong");
                }
            }


        } else if (update.hasCallbackQuery()) {
            String callbackQuery = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callbackQuery.equals(Button.YES_BUTTON.name())) {
                String text = "You have pressed YES button";
                processPressButton((int) messageId, chatId, text);
            } else if (callbackQuery.equals(Button.NO_BUTTON.name())) {
                String text = "You have pressed NO button";
                processPressButton((int) messageId, chatId, text);
            }
        }
    }


    private void processPressButton(int messageId, long chatId, String text) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId(messageId);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    private void register(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Do you want to register?");
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = getLists();

        inlineKeyboardMarkup.setKeyboard(rows);
        message.setReplyMarkup(inlineKeyboardMarkup);

        executeMessage(message);


    }

    private static List<List<InlineKeyboardButton>> getLists() {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        var yesButton = new InlineKeyboardButton();
        yesButton.setText("Yes");
        yesButton.setCallbackData(Button.YES_BUTTON.name());

        var noButton = new InlineKeyboardButton();
        noButton.setText("No");
        noButton.setCallbackData(Button.NO_BUTTON.name());

        rowInline.add(yesButton);
        rowInline.add(noButton);
        rows.add(rowInline);
        return rows;
    }

    public User getUser(Message message) {
        long chatId = message.getChatId();
        Optional<User> user = userRepository.findById(chatId);
        return user.get();
    }

    @Transactional
    public boolean deleteUser(Message message) {
        long chatId = message.getChatId();
        Optional<User> user = userRepository.findById(chatId);
        if (user.isPresent()) {
            userRepository.delete(user.get());
            return true;
        } else {
            return false;
        }
    }


    @Transactional
    public void registerUser(Message message) {
        long chatId = message.getChatId();
        Optional<User> existingUserOpt = userRepository.findById(chatId);

        var chat = message.getChat();
        User user;

        if (existingUserOpt.isEmpty()) {
            user = new User();
            user.setId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Date());

            userRepository.save(user);
            log.info("User is saved: {}", user);
        } else {
            user = existingUserOpt.get();
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Date());

            try {
                userRepository.save(user);
                log.info("User is updated: {}", user);
            } catch (ObjectOptimisticLockingFailureException e) {
                log.error("Failed to update user due to optimistic locking failure: {}", e.getMessage());
            }
        }
    }


    private void startCommandReceived(long chatId, String username) {

        String answer = EmojiParser.parseToUnicode("Hi, " + username + ", nice to meet you!" +
                                                   "\uD83E\uDD21");
        log.info(answer);
        prepareAndSendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow keyboardRow = new KeyboardRow();

        keyboardRow.add("weather");
        keyboardRow.add("get random joke");

        keyboardRows.add(keyboardRow);
        keyboardRow = new KeyboardRow();
        keyboardRow.add("register");
        keyboardRow.add("Check my data");
        keyboardRow.add("delete my data");
        keyboardRows.add(keyboardRow);

        keyboardMarkup.setKeyboard(keyboardRows);

        sendMessage.setReplyMarkup(keyboardMarkup);

        executeMessage(sendMessage);
    }

    private void executeMessage(SendMessage sendMessage) {
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error(e.getMessage());
        }
    }

    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        executeMessage(sendMessage);
    }

    @Transactional
    public void getRandomJoke(Long chatId) {
        String jokeFromSites = JokesParser.getJokeFromSites();
        jokeRepository.save(new Joke(jokeFromSites));
        prepareAndSendMessage(chatId, jokeFromSites);
    }

    @Scheduled(cron = "* 0/10 * * * *")
    public void sendAds() {
        var ads = advertisementRepository.findAll();
        var users = userRepository.findAll();
        for (var ad : ads) {
            for (var user : users) {
                prepareAndSendMessage(user.getId(), ad.getName());
            }
        }
    }
}
