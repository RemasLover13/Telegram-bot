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
    private final OpenRouterService openRouterService;
    private final RateLimitService rateLimitService;

    public static final String HELP_TEXT = """
        🤖 *Доступные команды:*
        /start - начать работу
        /help - помощь
        /my_data - мои данные
        /delete_data - удалить данные
        /time - текущее время
        /joke - случайная шутка
        /ai - задать вопрос AI (30 запросов/день)
        /usage - мои лимиты
        /credits - остатки на OpenRouter (только для владельца)
        """;

    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);

    public TelegramBotService(TelegramBotConfig config,
                              UserRepository userRepository,
                              AdvertisementRepository advertisementRepository,
                              JokeRepository jokeRepository,
                              OpenRouterService openRouterService,
                              RateLimitService rateLimitService
                             ) {
        this.config = config;
        this.userRepository = userRepository;
        this.advertisementRepository = advertisementRepository;
        this.jokeRepository = jokeRepository;
        this.openRouterService = openRouterService;
        this.rateLimitService = rateLimitService;

        initializeBotCommands();
    }

    private void initializeBotCommands() {
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "начать работу"));
        listOfCommands.add(new BotCommand("/settings", "настройки"));
        listOfCommands.add(new BotCommand("/help", "помощь"));
        listOfCommands.add(new BotCommand("/my_data", "мои данные"));
        listOfCommands.add(new BotCommand("/delete_data", "удалить данные"));
        listOfCommands.add(new BotCommand("/time", "текущее время"));
        listOfCommands.add(new BotCommand("/joke", "случайная шутка"));
        listOfCommands.add(new BotCommand("/ai", "задать вопрос AI"));
        listOfCommands.add(new BotCommand("/usage", "мои лимиты"));
        listOfCommands.add(new BotCommand("/credits", "остатки OpenRouter"));

        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
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
    public void onRegister() {
        super.onRegister();
    }

    @Override
    @Transactional
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            Long userId = update.getMessage().getFrom().getId();

            log.info("ChatId: {}, UserId: {}, Message: {}", chatId, userId, messageText);

            // Регистрируем пользователя при любом сообщении
            registerUser(update.getMessage());

            if (messageText.contains("/send") && config.getBotOwner().equals(chatId)) {
                handleBroadcastMessage(messageText);
            } else if (messageText.startsWith("/ai")) {
                handleAiRequest(chatId, userId, messageText);
            } else if (messageText.equals("/credits")) {
                handleCreditsCommand(chatId);
            } else {
                handleRegularCommands(chatId, userId, messageText, update.getMessage());
            }
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update);
        }
    }

    private void handleBroadcastMessage(String messageText) {
        var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
        var users = userRepository.findAll();
        for (var user : users) {
            prepareAndSendMessage(user.getId(), textToSend);
        }
        log.info("Broadcast message sent to {} users", users.size());
    }

    private void handleRegularCommands(long chatId, Long userId, String messageText, Message message) {
        switch (messageText) {
            case "/start":
                startCommandReceived(chatId, message.getChat().getFirstName());
                break;
            case "/help":
                prepareAndSendMessage(chatId, HELP_TEXT);
                break;
            case "/my_data":
                User user = getUser(message);
                if (user != null) {
                    prepareAndSendMessage(chatId, formatUserData(user));
                } else {
                    prepareAndSendMessage(chatId, "Пользователь не найден");
                }
                break;
            case "/delete_data":
                boolean isSuccess = deleteUser(message);
                if (isSuccess) {
                    prepareAndSendMessage(chatId, "✅ Данные успешно удалены");
                } else {
                    prepareAndSendMessage(chatId, "❌ Ошибка удаления данных");
                }
                break;
            case "/time":
                showCurrentTime(chatId);
                break;
            case "/register":
                register(chatId);
                break;
            case "/joke":
                getRandomJoke(chatId);
                break;
            case "/usage":
                String usageInfo = rateLimitService.getUsageInfo(userId);
                prepareAndSendMessage(chatId, usageInfo);
                break;
            default:
                // Автоматически отвечаем через AI на любые сообщения
                handleAiRequest(chatId, userId, messageText);
        }
    }

    private void handleCallbackQuery(Update update) {
        String callbackQuery = update.getCallbackQuery().getData();
        long messageId = update.getCallbackQuery().getMessage().getMessageId();
        long chatId = update.getCallbackQuery().getMessage().getChatId();

        if (callbackQuery.equals(Button.YES_BUTTON.name())) {
            String text = "✅ Вы нажали кнопку ДА - регистрация выполнена!";
            processPressButton((int) messageId, chatId, text);
        } else if (callbackQuery.equals(Button.NO_BUTTON.name())) {
            String text = "❌ Вы нажали кнопку НЕТ - регистрация отменена.";
            processPressButton((int) messageId, chatId, text);
        }
    }

    private void handleCreditsCommand(long chatId) {
        if (config.getBotOwner().equals(chatId)) {
            String creditsInfo = rateLimitService.getUsageInfo();
            prepareAndSendMessage(chatId, creditsInfo);
        } else {
            prepareAndSendMessage(chatId, "❌ Эта команда только для владельца бота");
        }
    }

    private void handleAiRequest(long chatId, Long userId, String messageText) {
        String question = extractQuestion(messageText);

        if (question.isEmpty()) {
            prepareAndSendMessage(chatId, "❓ Пожалуйста, введите ваш вопрос");
            return;
        }

        // Проверка лимита
        if (!rateLimitService.canMakeRequest(userId)) {
            prepareAndSendMessage(chatId,
                    "❌ Лимит AI-запросов исчерпан (30/день). Попробуйте завтра!\n" +
                    "Используйте /usage для проверки лимитов");
            return;
        }

        int remaining = rateLimitService.getRemainingRequests(userId);

        try {
            // Показываем, что бот думает
            SendMessage thinkingMsg = new SendMessage();
            thinkingMsg.setChatId(String.valueOf(chatId));
            thinkingMsg.setText("🤔 Думаю над ответом... (осталось запросов: " + remaining + ")");
            execute(thinkingMsg);

            // Генерация ответа через OpenRouter
            String response = openRouterService.generateResponse(question);
            prepareAndSendMessage(chatId, response);

            log.info("AI response generated for user {} (remaining: {})", userId, remaining - 1);

        } catch (Exception e) {
            log.error("AI request error for user {}: {}", userId, e.getMessage(), e);
            prepareAndSendMessage(chatId, "⚠️ Ошибка при обращении к AI. Попробуйте позже.\n\nОшибка: " + e.getMessage());
        }
    }

    private String extractQuestion(String messageText) {
        if (messageText.startsWith("/ai")) {
            return messageText.length() > 4 ? messageText.substring(4).trim() : "";
        }
        return messageText.trim();
    }

    private void showCurrentTime(long chatId) {
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String formattedTime = localDateTime.format(formatter);
        prepareAndSendMessage(chatId, "⏰ Текущее время: " + formattedTime);
    }

    private void processPressButton(int messageId, long chatId, String text) {
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId(messageId);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error processing button press: {}", e.getMessage());
        }
    }

    private void register(long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Вы хотите зарегистрироваться в системе?");

        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        var yesButton = new InlineKeyboardButton();
        yesButton.setText("✅ Да");
        yesButton.setCallbackData(Button.YES_BUTTON.name());

        var noButton = new InlineKeyboardButton();
        noButton.setText("❌ Нет");
        noButton.setCallbackData(Button.NO_BUTTON.name());

        rowInline.add(yesButton);
        rowInline.add(noButton);
        rows.add(rowInline);
        inlineKeyboardMarkup.setKeyboard(rows);
        message.setReplyMarkup(inlineKeyboardMarkup);

        executeMessage(message);
    }

    public User getUser(Message message) {
        long chatId = message.getChatId();
        Optional<User> user = userRepository.findById(chatId);
        return user.orElse(null);
    }

    @Transactional
    public boolean deleteUser(Message message) {
        long chatId = message.getChatId();
        Optional<User> user = userRepository.findById(chatId);
        if (user.isPresent()) {
            userRepository.delete(user.get());
            log.info("User deleted: {}", chatId);
            return true;
        } else {
            log.warn("User not found for deletion: {}", chatId);
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
            log.info("New user registered: {}", user);
        } else {
            user = existingUserOpt.get();
            // Обновляем данные пользователя если они изменились
            if (!user.getFirstName().equals(chat.getFirstName()) ||
                !user.getLastName().equals(chat.getLastName()) ||
                !user.getUserName().equals(chat.getUserName())) {

                user.setFirstName(chat.getFirstName());
                user.setLastName(chat.getLastName());
                user.setUserName(chat.getUserName());
                user.setRegisteredAt(new Date());

                try {
                    userRepository.save(user);
                    log.info("User data updated: {}", user);
                } catch (ObjectOptimisticLockingFailureException e) {
                    log.error("Failed to update user due to optimistic locking failure: {}", e.getMessage());
                }
            }
        }
    }

    private String formatUserData(User user) {
        return """
            👤 *Ваши данные:*
            
            • **ID:** %d
            • **Имя:** %s
            • **Фамилия:** %s
            • **Username:** @%s
            • **Зарегистрирован:** %s
            """.formatted(
                user.getId(),
                user.getFirstName() != null ? user.getFirstName() : "Не указано",
                user.getLastName() != null ? user.getLastName() : "Не указано",
                user.getUserName() != null ? user.getUserName() : "Не указано",
                user.getRegisteredAt().toString()
        );
    }

    private void startCommandReceived(long chatId, String username) {
        String answer = EmojiParser.parseToUnicode(
                "Привет, " + username + "! 👋\n\n" +
                "Я ваш AI-помощник с интеграцией OpenRouter.\n" +
                "✨ *Что я умею:*\n" +
                "• Отвечать на любые вопросы через AI\n" +
                "• Показывать текущее время\n" +
                "• Рассказывать случайные шутки\n" +
                "• Хранить ваши данные\n\n" +
                "🚀 *Доступно 30 AI-запросов в день*\n\n" +
                "Просто напишите мне вопрос или используйте /help для списка команд"
        );
        log.info("Start command for user: {}", username);
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        sendMessage.setParseMode("Markdown");

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboardRows = new ArrayList<>();

        // Первый ряд
        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("🎭 Шутка");
        keyboardRow.add("🤖 AI помощь");
        keyboardRows.add(keyboardRow);

        // Второй ряд
        keyboardRow = new KeyboardRow();
        keyboardRow.add("⏰ Время");
        keyboardRow.add("📊 Лимиты");
        keyboardRow.add("ℹ️ Помощь");
        keyboardRows.add(keyboardRow);

        keyboardMarkup.setKeyboard(keyboardRows);
        sendMessage.setReplyMarkup(keyboardMarkup);

        executeMessage(sendMessage);
    }

    private void executeMessage(SendMessage sendMessage) {
        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            log.error("Error sending message: {}", e.getMessage());
        }
    }

    private void prepareAndSendMessage(long chatId, String textToSend) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(String.valueOf(chatId));
        sendMessage.setText(textToSend);
        sendMessage.setParseMode("Markdown");
        executeMessage(sendMessage);
    }

    @Transactional
    public void getRandomJoke(Long chatId) {
        try {
            String jokeFromSites = JokesParser.getJokeFromSites();
            if (jokeFromSites != null && !jokeFromSites.isEmpty()) {
                jokeRepository.save(new Joke(jokeFromSites));
                prepareAndSendMessage(chatId, "😂 " + jokeFromSites);
                log.info("Joke sent to user: {}", chatId);
            } else {
                prepareAndSendMessage(chatId, "😅 Не удалось получить шутку. Попробуйте ещё раз!");
            }
        } catch (Exception e) {
            log.error("Error getting random joke: {}", e.getMessage(), e);
            prepareAndSendMessage(chatId, "⚠️ Извините, не удалось получить шутку. Попробуйте позже.");
        }
    }

    // Метод для административных уведомлений
    public void sendAdminNotification(String message) {
        if (config.getBotOwner() != null) {
            prepareAndSendMessage(config.getBotOwner(), "🔔 " + message);
        }
    }

    // Планировщик для ежедневного сброса лимитов (уже есть в RateLimitService)
    @Scheduled(cron = "0 0 0 * * ?")
    public void dailyResetNotification() {
        sendAdminNotification("Ежедневный сброс лимитов выполнен. Все пользователи получили 30 AI-запросов.");
    }
}