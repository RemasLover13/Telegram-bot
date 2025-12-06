package com.remaslover.telegrambotaq.service;


import com.remaslover.telegrambotaq.config.TelegramBotConfig;
import com.remaslover.telegrambotaq.entity.User;
import com.remaslover.telegrambotaq.enums.Button;
import com.remaslover.telegrambotaq.repository.UserRepository;
import com.remaslover.telegrambotaq.util.JokesParser;
import com.vdurmont.emoji.EmojiParser;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
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
import java.util.*;

@Service
public class TelegramBotService extends TelegramLongPollingBot {

    private final TelegramBotConfig config;
    private final UserRepository userRepository;
    private final OpenRouterService openRouterService;
    private final RateLimitService rateLimitService;
    private final OpenRouterLimitService openRouterLimitService;
    private final NewsApiService newsApiService;
    @PersistenceContext
    private final EntityManager entityManager;
    private final TransactionTemplate transactionTemplate;

    public static final String HELP_TEXT = """
            🤖 *Доступные команды:*
            /start - начать работу
            /help - помощь
            /my_data - мои данные
            /delete_data - удалить данные
            /time - текущее время
            /joke - случайная шутка
            /ai - задать вопрос AI (5 запросов/день)
            /usage - мои лимиты
            /credits - остатки на OpenRouter (только для владельца)
                        
            📰 *Новости:*
            /topnews [страна] [категория] - главные новости
            /news_category [категория] - новости по категории
            /news_country [страна] - новости по стране
            /news_search [запрос] - поиск новостей
                        
            🌍 *Примеры:*
            /topnews сша технологии
            /news_category спорт
            /news_country германия
            /news_search искусственный интеллект
            """;

    private static final Logger log = LoggerFactory.getLogger(TelegramBotService.class);

    public TelegramBotService(TelegramBotConfig config,
                              UserRepository userRepository,
                              OpenRouterService openRouterService,
                              RateLimitService rateLimitService,
                              OpenRouterLimitService openRouterLimitService,
                              NewsApiService newsApiService,
                              EntityManager entityManager,
                              TransactionTemplate transactionTemplate) {
        super(config.getBotToken());
        this.config = config;
        this.userRepository = userRepository;
        this.openRouterLimitService = openRouterLimitService;
        this.openRouterService = openRouterService;
        this.rateLimitService = rateLimitService;
        this.newsApiService = newsApiService;
        this.entityManager = entityManager;
        this.transactionTemplate = transactionTemplate;

        initializeBotCommands();
    }

    private void initializeBotCommands() {
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "начать работу"));
        listOfCommands.add(new BotCommand("/help", "помощь"));
        listOfCommands.add(new BotCommand("/my_data", "мои данные"));
        listOfCommands.add(new BotCommand("/delete_data", "удалить данные"));
        listOfCommands.add(new BotCommand("/time", "текущее время"));
        listOfCommands.add(new BotCommand("/joke", "случайная шутка"));
        listOfCommands.add(new BotCommand("/ai", "задать вопрос AI"));
        listOfCommands.add(new BotCommand("/usage", "мои лимиты"));
        listOfCommands.add(new BotCommand("/credits", "остатки OpenRouter"));
        listOfCommands.add(new BotCommand("/topnews", "главные новости"));
        listOfCommands.add(new BotCommand("/news_category", "новости по категории"));
        listOfCommands.add(new BotCommand("/news_country", "новости по стране"));
        listOfCommands.add(new BotCommand("/news_search", "поиск новостей"));

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
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();
            Long userId = update.getMessage().getFrom().getId();

            log.info("ChatId: {}, UserId: {}, Message: {}", chatId, userId, messageText);

            registerUser(update.getMessage());

            if (messageText.contains("/send") && config.getBotOwner().equals(chatId)) {
                handleBroadcastMessage(messageText);
            } else if (messageText.startsWith("/ai")) {
                handleAiRequest(chatId, userId, messageText);
            } else if (messageText.startsWith("/topnews")) {
                handleTopNewsCommand(chatId, messageText);
            } else if (messageText.startsWith("/news_category")) {
                handleNewsCategoryCommand(chatId, messageText);
            } else if (messageText.startsWith("/news_country")) {
                handleNewsCountryCommand(chatId, messageText);
            } else if (messageText.startsWith("/news_search")) {
                handleNewsSearchCommand(chatId, messageText);
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
            case "ℹ️ Помощь":
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
            case "⏰ Время":
                showCurrentTime(chatId);
                break;
            case "/register":
                register(chatId);
                break;
            case "/joke":
            case "🎭 Шутка":
                getRandomJoke(chatId);
                break;
            case "/usage":
            case "📊 Лимиты":
                String usageInfo = rateLimitService.getUsageInfo(userId);
                prepareAndSendMessage(chatId, usageInfo);
                break;
            case "📰 Новости":
                showNewsHelp(chatId);
                break;
            case "🔥 Главные новости":
                handleTopNewsCommand(chatId, "/topnews");
                break;
            case "🌍 Новости страны":
                handleNewsCountryCommand(chatId, "/news_country");
                break;
            case "📋 Новости категории":
                handleNewsCategoryCommand(chatId, "/news_category");
                break;
            case "🔍 Поиск новостей":
                handleNewsSearchCommand(chatId, "/news_search");
                break;
            case "🤖 AI помощь":
                prepareAndSendMessage(chatId, "💡 Напишите ваш вопрос и я отвечу с помощью AI!");
                break;
            default:
                if (!messageText.startsWith("/")) {
                    handleAiRequest(chatId, userId, messageText);
                } else {
                    prepareAndSendMessage(chatId, "❓ Неизвестная команда. Используйте /help для списка команд.");
                }
        }
    }

    private void showNewsHelp(long chatId) {
        String newsHelp = """
                📰 *Новостные команды:*
                                
                • /topnews [страна] [категория] - главные новости
                • /news_category [категория] - новости по категории
                • /news_country [страна] - новости по стране
                • /news_search [запрос] - поиск новостей
                                
                🌍 *Примеры:*
                /topnews сша технологии
                /news_category спорт
                /news_country германия
                /news_search искусственный интеллект
                                
                📋 *Доступные категории:*
                общее, бизнес, развлечения, здоровье, наука, спорт, технологии
                                
                🌐 *Доступные страны:*
                россия, сша, великобритания, германия, франция, китай, украина
                """;
        prepareAndSendMessage(chatId, newsHelp);
    }

    /**
     * Обработка команды /topnews [страна] [категория]
     * Примеры:
     * /topnews - главные новости России
     * /topnews сша - главные новости США
     * /topnews сша технологии - технологии в США
     * /topnews россия спорт - спорт в России
     */
    private void handleTopNewsCommand(long chatId, String messageText) {
        String[] parts = messageText.split(" ");

        try {
            if (parts.length == 1) {
                prepareAndSendMessage(chatId, "📡 Получаю главные новости России...");
                String news = newsApiService.getTopHeadlinesForCountry("Россия", 5);
                prepareAndSendMessage(chatId, news);

            } else if (parts.length == 2) {
                String country = parts[1];
                prepareAndSendMessage(chatId, "📡 Получаю главные новости для " + country + "...");
                String news = newsApiService.getTopHeadlinesForCountry(country, 5);
                prepareAndSendMessage(chatId, news);

            } else if (parts.length >= 3) {
                String country = parts[1];
                String category = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));
                prepareAndSendMessage(chatId, "📡 Получаю новости категории '" + category + "' для " + country + "...");
                String news = newsApiService.getTopHeadlinesForCountryAndCategory(country, category, 5);
                prepareAndSendMessage(chatId, news);
            }
        } catch (Exception e) {
            log.error("Error handling top news command: {}", e.getMessage(), e);
            prepareAndSendMessage(chatId, "⚠️ Ошибка при получении новостей. Попробуйте позже.");
        }
    }

    /**
     * Обработка команды /news_category [категория]
     * Примеры:
     * /news_category технологии
     * /news_category спорт
     * /news_category бизнес
     */
    private void handleNewsCategoryCommand(long chatId, String messageText) {
        String[] parts = messageText.split(" ");

        if (parts.length == 1) {
            // Список доступных категорий
            String categories = """
                    📋 *Доступные категории новостей:*
                                        
                    • общее
                    • бизнес
                    • развлечения
                    • здоровье
                    • наука
                    • спорт
                    • технологии
                                        
                    *Использование:* /news_category [категория]
                    *Пример:* /news_category технологии
                    """;
            prepareAndSendMessage(chatId, categories);

        } else {
            try {
                String category = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                prepareAndSendMessage(chatId, "📡 Получаю новости категории '" + category + "'...");
                String news = newsApiService.getTopHeadlinesForCategory(category, 5);
                prepareAndSendMessage(chatId, news);
            } catch (Exception e) {
                log.error("Error handling news category command: {}", e.getMessage(), e);
                prepareAndSendMessage(chatId, "⚠️ Ошибка при получении новостей. Проверьте название категории.");
            }
        }
    }

    /**
     * Обработка команды /news_country [страна]
     * Примеры:
     * /news_country сша
     * /news_country германия
     * /news_country китай
     */
    private void handleNewsCountryCommand(long chatId, String messageText) {
        String[] parts = messageText.split(" ");

        if (parts.length == 1) {
            // Список доступных стран
            String countries = """
                    🌍 *Доступные страны:*
                                        
                    • россия (ru)
                    • сша (us)
                    • великобритания (gb)
                    • германия (de)
                    • франция (fr)
                    • китай (cn)
                    • украина (ua)
                                        
                    *Использование:* /news_country [страна]
                    *Пример:* /news_country сша
                    """;
            prepareAndSendMessage(chatId, countries);

        } else {
            try {
                String country = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                prepareAndSendMessage(chatId, "📡 Получаю новости для " + country + "...");
                String news = newsApiService.getTopHeadlinesForCountry(country, 5);
                prepareAndSendMessage(chatId, news);
            } catch (Exception e) {
                log.error("Error handling news country command: {}", e.getMessage(), e);
                prepareAndSendMessage(chatId, "⚠️ Ошибка при получении новостей. Проверьте название страны.");
            }
        }
    }

    /**
     * Обработка команды /news_search [запрос]
     * Примеры:
     * /news_search искусственный интеллект
     * /news_search криптовалюта
     * /news_search политика
     */
    private void handleNewsSearchCommand(long chatId, String messageText) {
        String[] parts = messageText.split(" ");

        if (parts.length == 1) {
            prepareAndSendMessage(chatId,
                    "🔍 *Поиск новостей*\n\n" +
                    "*Использование:* /news_search [запрос]\n" +
                    "*Пример:* /news_search искусственный интеллект\n\n" +
                    "Я найду самые свежие новости по вашему запросу.");

        } else {
            try {
                String query = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                prepareAndSendMessage(chatId, "🔍 Ищу новости по запросу: " + query + "...");

                String news = newsApiService.searchNews(query, 5);
                prepareAndSendMessage(chatId, news);

            } catch (Exception e) {
                log.error("Error handling news search command: {}", e.getMessage(), e);
                prepareAndSendMessage(chatId, "⚠️ Ошибка при поиске новостей. Попробуйте другой запрос.");
            }
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
            String creditsInfo = openRouterLimitService.getUsageInfo();
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

        if (!rateLimitService.canMakeAiRequest(userId)) {
            prepareAndSendMessage(chatId,
                    "❌ Лимит AI-запросов исчерпан (5/день). Попробуйте завтра!\n" +
                    "Используйте /usage для проверки лимитов");
            return;
        }

        int remaining = rateLimitService.getRemainingAiRequests(userId);

        try {
            SendMessage thinkingMsg = new SendMessage();
            thinkingMsg.setChatId(String.valueOf(chatId));
            thinkingMsg.setText("🤔 Думаю над ответом... (осталось AI запросов: " + remaining + ")");
            execute(thinkingMsg);

            String response = openRouterService.generateResponse(question);
            prepareAndSendMessage(chatId, response);

            log.info("AI response generated for user {} (remaining: {})", userId, remaining - 1);

        } catch (Exception e) {
            log.error("AI request error for user {}: {}", userId, e.getMessage(), e);
            prepareAndSendMessage(chatId, "⚠️ Ошибка при обращении к AI. Попробуйте позже.");
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
        Optional<com.remaslover.telegrambotaq.entity.User> user = userRepository.findById(chatId);
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

    public void registerUser(Message message) {
        transactionTemplate.execute(status -> {
            long chatId = message.getChatId();

            User user = userRepository.findById(chatId)
                    .orElseGet(() -> {
                        var chat = message.getChat();
                        User newUser = new User();
                        newUser.setId(chatId);
                        newUser.setFirstName(chat.getFirstName());
                        newUser.setLastName(chat.getLastName());
                        newUser.setUserName(chat.getUserName());
                        newUser.setRegisteredAt(new Date());

                        return userRepository.save(newUser);
                    });

            entityManager.lock(user, LockModeType.PESSIMISTIC_WRITE);

            if (!Objects.equals(user.getFirstName(), message.getChat().getFirstName()) ||
                !Objects.equals(user.getLastName(), message.getChat().getLastName()) ||
                !Objects.equals(user.getUserName(), message.getChat().getUserName())) {

                user.setFirstName(message.getChat().getFirstName());
                user.setLastName(message.getChat().getLastName());
                user.setUserName(message.getChat().getUserName());
                user.setRegisteredAt(new Date());
            }

            userRepository.save(user);
            log.debug("User processed: {}", user.getId());

            return null;
        });
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
                "• Получать актуальные новости 📰\n" +
                "• Хранить ваши данные\n\n" +
                "🚀 *Доступно 5 AI-запросов в день*\n" +
                "🌍 *Новости из 50+ стран и 7 категорий*\n\n" +
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

        KeyboardRow row1 = new KeyboardRow();
        row1.add("🎭 Шутка");
        row1.add("🤖 AI помощь");
        row1.add("📰 Новости");
        keyboardRows.add(row1);

        KeyboardRow row2 = new KeyboardRow();
        row2.add("⏰ Время");
        row2.add("📊 Лимиты");
        row2.add("ℹ️ Помощь");
        keyboardRows.add(row2);

        KeyboardRow row3 = new KeyboardRow();
        row3.add("🌍 Новости страны");
        row3.add("📋 Новости категории");
        row3.add("🔥 Главные новости");
        keyboardRows.add(row3);

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

    public void sendAdminNotification(String message) {
        if (config.getBotOwner() != null) {
            prepareAndSendMessage(config.getBotOwner(), "🔔 " + message);
        }
    }
}