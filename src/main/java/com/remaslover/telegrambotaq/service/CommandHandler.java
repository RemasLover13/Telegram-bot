package com.remaslover.telegrambotaq.service;

import com.remaslover.telegrambotaq.config.TelegramBotConfig;
import com.remaslover.telegrambotaq.entity.User;
import com.remaslover.telegrambotaq.exception.JokeNotFoundException;
import com.remaslover.telegrambotaq.util.TelegramMarkdownEscapeUtil;
import com.vdurmont.emoji.EmojiParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

@Component
public class CommandHandler {
    private final UserService userService;
    private final RateLimitService rateLimitService;
    private final JokerService jokerService;
    private final NewsApiService newsApiService;
    private final OpenRouterService openRouterService;
    private final OpenRouterLimitService openRouterLimitService;
    private final TelegramBotConfig config;
    private final MessageSender messageSender;
    private final KeyboardManager keyboardManager;
    private final ConversationContextService conversationContextService;
    private final MessageQueueService messageQueueService;

    private static final Logger log = LoggerFactory.getLogger(CommandHandler.class);

    public static final String HELP_TEXT = """
            🤖 *Доступные команды:*
                        
            *Основные:*
            /start - начать работу
            /help - помощь
            /my_data - мои данные
            /delete_data - удалить данные
                        
            *AI и контекст:*
            /ai [вопрос] - задать вопрос AI
            /context - управление контекстом разговора
                        
            *Информация:*
            /time - текущее время
            /joke - случайная шутка
            /usage - мои лимиты
            /credits - остатки OpenRouter (только для владельца)
                        
            *Новости:*
            /topnews [страна] [категория] - главные новости
            /news_category [категория] - новости по категории
            /news_country [страна] - новости по стране
            /news_search [запрос] - поиск новостей
                        
            ✨ *Бот помнит контекст разговора (последние 10 сообщений)*
            """;

    public CommandHandler(UserService userService,
                          RateLimitService rateLimitService,
                          JokerService jokerService,
                          NewsApiService newsApiService,
                          OpenRouterService openRouterService,
                          OpenRouterLimitService openRouterLimitService,
                          TelegramBotConfig config,
                          MessageSender messageSender,
                          KeyboardManager keyboardManager, ConversationContextService conversationContextService, MessageQueueService messageQueueService) {
        this.messageSender = messageSender;
        this.keyboardManager = keyboardManager;
        this.userService = userService;
        this.rateLimitService = rateLimitService;
        this.jokerService = jokerService;
        this.newsApiService = newsApiService;
        this.openRouterService = openRouterService;
        this.openRouterLimitService = openRouterLimitService;
        this.config = config;
        this.conversationContextService = conversationContextService;
        this.messageQueueService = messageQueueService;
    }

    private void sendMessage(long chatId, String text) {
        messageSender.sendMessage(chatId, text);
    }

    private void sendMessageWithKeyboard(long chatId, String text) {
        ReplyKeyboardMarkup keyboard = keyboardManager.createMainKeyboard();
        messageSender.sendMessageWithKeyboard(chatId, text, keyboard);
    }



    public void handleRegularCommands(long chatId, Long userId, String messageText, Message message) {
        switch (messageText) {
            case "/start":
                startCommandReceived(chatId, message.getChat().getFirstName());
                break;
            case "/help":
            case "ℹ️ Помощь":
                sendMessage(chatId, HELP_TEXT);
                break;
            case "/my_data":
                handleMyDataCommand(chatId, message);
                break;
            case "/delete_data":
                handleDeleteDataCommand(chatId, message);
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
                handleUsageCommand(chatId, userId);
                break;
            case "/context":
            case "🧠 Контекст":
                showContextMenu(chatId);
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
                sendMessage(chatId, "💡 Напишите ваш вопрос и я отвечу с учетом контекста разговора!");
                break;
            default:
                if (!messageText.startsWith("/")) {
                    handleAiRequest(chatId, userId, messageText);
                } else {
                    sendMessage(chatId, "❓ Неизвестная команда. Используйте /help для списка команд.");
                }
        }
    }

    public void handleNewsCategoryCommand(long chatId, String messageText) {
        String normalizedText = messageText.replace("/newscategory", "/news_category");
        String[] parts = normalizedText.split(" ");

        if (parts.length == 1) {
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
                    *Или:* /newscategory технологии
                    """;
            sendMessage(chatId, categories);
        } else {
            try {
                String category = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                sendMessage(chatId, "📡 Получаю новости категории '" + category + "'...");
                String news = newsApiService.getTopHeadlinesForCategory(category, 5);
                sendMessage(chatId, news);
            } catch (Exception e) {
                log.error("Error handling news category command: {}", e.getMessage(), e);
                sendMessage(chatId, "⚠️ Ошибка при получении новостей. Проверьте название категории.");
            }
        }
    }

    public void handleNewsCountryCommand(long chatId, String messageText) {
        String normalizedText = messageText.replace("/newscountry", "/news_country");
        String[] parts = normalizedText.split(" ");

        if (parts.length == 1) {
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
                    *Или:* /newscountry сша
                    """;
            sendMessage(chatId, countries);
        } else {
            try {
                String country = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                sendMessage(chatId, "📡 Получаю новости для " + country + "...");
                String news = newsApiService.getTopHeadlinesForCountry(country, 5);
                sendMessage(chatId, news);
            } catch (Exception e) {
                log.error("Error handling news country command: {}", e.getMessage(), e);
                sendMessage(chatId, "⚠️ Ошибка при получении новостей. Проверьте название страны.");
            }
        }
    }

    public void handleNewsSearchCommand(long chatId, String messageText) {
        String normalizedText = messageText.replace("/newssearch", "/news_search");
        String[] parts = normalizedText.split(" ");

        if (parts.length == 1) {
            sendMessage(chatId,
                    "🔍 *Поиск новостей*\n\n" +
                    "*Использование:* /news_search [запрос]\n" +
                    "*Пример:* /news_search искусственный интеллект\n" +
                    "*Или:* /newssearch искусственный интеллект\n\n" +
                    "Я найду самые свежие новости по вашему запросу.");
        } else {
            try {
                String query = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                sendMessage(chatId, "🔍 Ищу новости по запросу: " + query + "...");
                String news = newsApiService.searchNews(query, 5);
                sendMessage(chatId, news);
            } catch (Exception e) {
                log.error("Error handling news search command: {}", e.getMessage(), e);
                sendMessage(chatId, "⚠️ Ошибка при поиске новостей. Попробуйте другой запрос.");
            }
        }
    }

    /**
     * Обработка callback-запросов от inline клавиатуры
     */
    public void handleCallbackQuery(CallbackQuery callbackQuery) {
        try {
            long chatId = callbackQuery.getMessage().getChatId();
            Long userId = callbackQuery.getFrom().getId();
            String callbackData = callbackQuery.getData();
            Integer messageId = callbackQuery.getMessage().getMessageId();

            log.info("Received callback query from chat {}: {}", chatId, callbackData);

            AnswerCallbackQuery answer = new AnswerCallbackQuery();
            answer.setCallbackQueryId(callbackQuery.getId());


            if (callbackData != null && callbackData.startsWith("/context")) {
                answer.setText("✅ Обрабатываю команду...");
                try {
                    messageSender.getBot().execute(answer);
                } catch (Exception e) {
                    log.warn("Could not send callback answer: {}", e.getMessage());
                }


                handleContextCommand(chatId, userId, callbackData);

            } else if (callbackData != null && callbackData.startsWith("/news")) {

                answer.setText("📰 Получаю новости...");
                try {
                    messageSender.getBot().execute(answer);
                } catch (Exception e) {
                    log.warn("Could not send callback answer: {}", e.getMessage());
                }

                handleNewsCallback(chatId, callbackData);

            } else if (callbackData != null) {
                handleOtherCallback(chatId, userId, callbackData, messageId);

            } else {
                answer.setText("❌ Неизвестная команда");
                try {
                    messageSender.getBot().execute(answer);
                } catch (Exception e) {
                    log.warn("Could not send callback answer: {}", e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error handling callback query: {}", e.getMessage(), e);
        }
    }

    /**
     * Обработка новостных callback-команд
     */
    private void handleNewsCallback(long chatId, String callbackData) {
        String[] parts = callbackData.split(" ");

        if (parts.length == 0) {
            return;
        }

        String command = parts[0];

        switch (command) {
            case "/news_category":
                if (parts.length > 1) {
                    String category = parts[1];
                    String fullCommand = "/news_category " + category;
                    handleNewsCategoryCommand(chatId, fullCommand);
                } else {
                    handleNewsCategoryCommand(chatId, "/news_category");
                }
                break;

            case "/news_country":
                if (parts.length > 1) {
                    String country = parts[1];
                    String fullCommand = "/news_country " + country;
                    handleNewsCountryCommand(chatId, fullCommand);
                } else {
                    handleNewsCountryCommand(chatId, "/news_country");
                }
                break;

            case "/news_search":
                if (parts.length > 1) {
                    String query = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                    String fullCommand = "/news_search " + query;
                    handleNewsSearchCommand(chatId, fullCommand);
                } else {
                    handleNewsSearchCommand(chatId, "/news_search");
                }
                break;

            case "/topnews":
                if (parts.length > 1) {
                    String country = parts[1];
                    String category = parts.length > 2 ? parts[2] : "";
                    String fullCommand = "/topnews " + country + " " + category;
                    handleTopNewsCommand(chatId, fullCommand);
                } else {
                    handleTopNewsCommand(chatId, "/topnews");
                }
                break;
        }
    }

    /**
     * Обработка других callback-данных
     */
    private void handleOtherCallback(long chatId, Long userId, String callbackData, Integer messageId) {

        if ("BUTTON_YES".equals(callbackData) || "BUTTON_NO".equals(callbackData)) {
            String response = callbackData.equals("BUTTON_YES")
                    ? "✅ Вы согласились на регистрацию!"
                    : "❌ Вы отказались от регистрации";

            sendMessage(chatId, response);

            if (messageId != null) {
                try {
                    messageSender.editMessage(chatId, messageId, "Ваш выбор принят!");
                } catch (Exception e) {
                    log.warn("Could not edit message: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Показывает меню контекста с inline клавиатурой
     */
    public void showContextMenu(long chatId) {
        String menuText = """
                🧠 *Управление контекстом разговора*
                
                Выберите действие:
                """;

        InlineKeyboardMarkup keyboard = keyboardManager.createContextKeyboard();
        messageSender.sendMessageWithInlineKeyboard(chatId, menuText, keyboard);
    }

    /**
     * Обновленный метод управления контекстом
     */
    public void handleContextCommand(long chatId, Long userId, String messageText) {
        String[] parts = messageText.split(" ");

        if (parts.length == 1) {
            showContextMenu(chatId);

        } else {
            String subCommand = parts[1].toLowerCase();

            switch (subCommand) {
                case "clear":
                    openRouterService.clearConversationHistory(userId);
                    sendMessage(chatId, "✅ История разговора очищена");
                    log.info("User {} cleared conversation history", userId);
                    break;

                case "show":
                    try {
                        String history = openRouterService.getConversationHistorySimple(userId);
                        sendMessage(chatId, history);
                    } catch (Exception e) {
                        log.error("Error showing context for user {}: {}", userId, e.getMessage());
                        sendMessage(chatId, "⚠️ Ошибка при получении истории.");
                    }
                    break;

                case "show_md":
                    try {
                        String history = openRouterService.getConversationHistory(userId);
                        sendMessage(chatId, history);
                    } catch (Exception e) {
                        log.warn("Markdown context failed for user {}, falling back: {}",
                                userId, e.getMessage());
                        String history = openRouterService.getConversationHistorySimple(userId);
                        sendMessage(chatId, history);
                    }
                    break;

                case "show_debug":
                    try {
                        String history = openRouterService.getConversationHistoryDebug(userId);
                        sendMessage(chatId, history);
                    } catch (Exception e) {
                        log.error("Error showing debug context for user {}: {}", userId, e.getMessage());
                        sendMessage(chatId, "❌ Ошибка при получении отладочной истории.");
                    }
                    break;

                case "stats":
                    try {
                        String stats = openRouterService.getContextStats();
                        sendMessage(chatId, TelegramMarkdownEscapeUtil.escapeMarkdownV2(stats));
                    } catch (Exception e) {
                        log.error("Error showing stats for user {}: {}", userId, e.getMessage());
                        sendMessage(chatId, "❌ Ошибка при получении статистики.");
                    }
                    break;

                case "help":
                    handleContextCommand(chatId, userId, "/context");
                    break;

                default:
                    sendMessage(chatId, "❓ Неизвестная подкоманда. Используйте `/context help`");
            }
        }
    }

    private void handleMyDataCommand(long chatId, Message message) {
        User user = userService.getUser(message);
        if (user != null) {
            sendMessage(chatId, userService.formatUserData(user));
        } else {
            sendMessage(chatId, "Пользователь не найден");
        }
    }

    private void handleDeleteDataCommand(long chatId, Message message) {
        boolean isSuccess = userService.deleteUser(message);
        if (isSuccess) {
            sendMessage(chatId, "✅ Данные успешно удалены");
        } else {
            sendMessage(chatId, "❌ Ошибка удаления данных");
        }
    }

    private void handleUsageCommand(long chatId, Long userId) {
        String usageInfo = rateLimitService.getUsageInfo(userId);
        sendMessage(chatId, usageInfo);
    }

    private void showCurrentTime(long chatId) {
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String formattedTime = localDateTime.format(formatter);
        sendMessage(chatId, "⏰ Текущее время: " + formattedTime);
    }

    public void getRandomJoke(Long chatId) {
        try {
            String joke = jokerService.getJoke();
            sendMessage(chatId, "😂 " + joke);
            log.info("Joke sent to user: {}", chatId);
        } catch (JokeNotFoundException e) {
            sendMessage(chatId, "😅 Не удалось получить шутку. Попробуйте ещё раз!");
        } catch (Exception e) {
            log.error("Error getting random joke: {}", e.getMessage(), e);
            sendMessage(chatId, "⚠️ Извините, не удалось получить шутку. Попробуйте позже.");
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
        sendMessage(chatId, newsHelp);
    }

    public void handleTopNewsCommand(long chatId, String messageText) {
        String[] parts = messageText.split(" ");

        try {
            if (parts.length == 1) {
                sendMessage(chatId, "📡 Получаю главные новости USA...");
                String news = newsApiService.getTopHeadlinesForCountry("us", 5);
                sendMessage(chatId, news);
            } else if (parts.length == 2) {
                String country = parts[1];
                sendMessage(chatId, "📡 Получаю главные новости для " + country + "...");
                String news = newsApiService.getTopHeadlinesForCountry(country, 5);
                sendMessage(chatId, news);
            } else if (parts.length >= 3) {
                String country = parts[1];
                String category = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));
                sendMessage(chatId, "📡 Получаю новости категории '" + category + "' для " + country + "...");
                String news = newsApiService.getTopHeadlinesForCountryAndCategory(country, category, 5);
                sendMessage(chatId, news);
            }
        } catch (Exception e) {
            log.error("Error handling top news command: {}", e.getMessage(), e);
            sendMessage(chatId, "⚠️ Ошибка при получении новостей. Попробуйте позже.");
        }
    }


    /**
     * Обработка AI запросов с разбивкой и очередью
     */
    public void handleAiRequest(long chatId, Long userId, String messageText) {
        String question = extractQuestion(messageText);

        if (question.isEmpty()) {
            sendMessage(chatId, "❓ Пожалуйста, введите ваш вопрос");
            return;
        }

        if (!rateLimitService.canMakeAiRequest(userId)) {
            sendMessage(chatId,
                    "❌ Лимит AI-запросов исчерпан (5/день). Попробуйте завтра!\n" +
                    "Используйте /usage для проверки лимитов");
            return;
        }

        int remaining = rateLimitService.getRemainingAiRequests(userId);

        try {
            String thinkingText = "🤔 Думаю над ответом... (осталось AI запросов: " + remaining + ")";
            sendMessage(chatId, thinkingText);

            List<String> responseParts = openRouterService.generateResponseAsParts(userId, question);

            if (responseParts.isEmpty()) {
                sendMessage(chatId, "⚠️ Получен пустой ответ от AI. Попробуйте переформулировать вопрос.");
                return;
            }

            sendMessage(chatId, responseParts.get(0));

            if (responseParts.size() > 1) {
                String notice = String.format(
                        "📄 *Ответ состоит из %d частей. Отправляю продолжение...*",
                        responseParts.size()
                );

                messageQueueService.enqueueMessage(chatId, notice, 1000);

                for (int i = 1; i < responseParts.size(); i++) {
                    messageQueueService.enqueueMessage(chatId, responseParts.get(i), 1000 + (i * 1500));
                }
            }

            rateLimitService.registerAiRequest(userId);

            log.info("✅ AI response sent for user {} in {} parts (remaining: {})",
                    userId, responseParts.size(), remaining - 1);

        } catch (Exception e) {
            log.error("❌ AI request error for user {}: {}", userId, e.getMessage(), e);
            sendMessage(chatId, "⚠️ Ошибка при обращении к AI. Попробуйте позже.");
        }
    }


    public void handleCreditsCommand(long chatId) {
        if (config.getBotOwner().equals(chatId)) {
            String creditsInfo = openRouterLimitService.getUsageInfo();
            sendMessage(chatId, creditsInfo);
        } else {
            sendMessage(chatId, "❌ Эта команда только для владельца бота");
        }
    }

    /**
     * Обновленная команда /start с информацией о контексте
     */
    public void startCommandReceived(long chatId, String username) {
        String answer = EmojiParser.parseToUnicode(
                "Привет, " + username + "! 👋\n\n" +
                "Я ваш AI-помощник с *поддержкой контекста* разговора.\n" +
                "✨ *Что я умею:*\n" +
                "• Отвечать на вопросы с учетом истории диалога 🧠\n" +
                "• Помнить контекст (10 последних сообщений)\n" +
                "• Показывать текущее время\n" +
                "• Рассказывать случайные шутки\n" +
                "• Получать актуальные новости 📰\n" +
                "• Хранить ваши данные\n\n" +
                "🚀 *Доступно 5 AI-запросов в день*\n" +
                "🧠 *Контекст сохраняется 30 минут*\n" +
                "🌍 *Новости из 50+ стран и 7 категорий*\n\n" +
                "Используйте /context для управления историей разговора\n" +
                "Или просто напишите мне вопрос!"
        );
        log.info("Start command for user: {}", username);
        sendMessageWithKeyboard(chatId, answer);
    }

    public void register(long chatId) {
        String messageText = "Вы хотите зарегистрироваться в системе?";
        InlineKeyboardMarkup keyboard = keyboardManager.createRegistrationKeyboard();
        messageSender.sendMessageWithInlineKeyboard(chatId, messageText, keyboard);
    }

    private String extractQuestion(String messageText) {
        if (messageText.startsWith("/ai")) {
            return messageText.length() > 4 ? messageText.substring(4).trim() : "";
        }
        return messageText.trim();
    }

}