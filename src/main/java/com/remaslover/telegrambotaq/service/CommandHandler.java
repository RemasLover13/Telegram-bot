package com.remaslover.telegrambotaq.service;

import com.remaslover.telegrambotaq.config.TelegramBotConfig;
import com.remaslover.telegrambotaq.entity.User;
import com.remaslover.telegrambotaq.exception.JokeNotFoundException;
import com.vdurmont.emoji.EmojiParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

@Component
public class CommandHandler {

    @Lazy
    private final TelegramBotService telegramBotService;
    private final UserService userService;
    private final RateLimitService rateLimitService;
    private final JokerService jokerService;
    private final NewsApiService newsApiService;
    private final OpenRouterService openRouterService;
    private final OpenRouterLimitService openRouterLimitService;
    private final TelegramBotConfig config;


    private static final Logger log = LoggerFactory.getLogger(CommandHandler.class);

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

    public CommandHandler(TelegramBotService telegramBotService, UserService userService,
                          RateLimitService rateLimitService, JokerService jokerService,
                          NewsApiService newsApiService, OpenRouterService openRouterService,
                          OpenRouterLimitService openRouterLimitService, TelegramBotConfig config) {
        this.telegramBotService = telegramBotService;
        this.userService = userService;
        this.rateLimitService = rateLimitService;
        this.jokerService = jokerService;
        this.newsApiService = newsApiService;
        this.openRouterService = openRouterService;
        this.openRouterLimitService = openRouterLimitService;
        this.config = config;
    }

    public void handleRegularCommands(long chatId, Long userId, String messageText, Message message) {
        switch (messageText) {
            case "/start":
                startCommandReceived(chatId, message.getChat().getFirstName());
                break;
            case "/help":
            case "ℹ️ Помощь":
                telegramBotService.prepareAndSendMessage(chatId, HELP_TEXT);
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
                telegramBotService.register(chatId);
                break;
            case "/joke":
            case "🎭 Шутка":
                getRandomJoke(chatId);
                break;
            case "/usage":
            case "📊 Лимиты":
                handleUsageCommand(chatId, userId);
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
                telegramBotService.prepareAndSendMessage(chatId, "💡 Напишите ваш вопрос и я отвечу с помощью AI!");
                break;
            default:
                if (!messageText.startsWith("/")) {
                    handleAiRequest(chatId, userId, messageText);
                } else {
                    telegramBotService.prepareAndSendMessage(chatId, "❓ Неизвестная команда. Используйте /help для списка команд.");
                }
        }
    }

    private void handleMyDataCommand(long chatId, Message message) {
        User user = userService.getUser(message);
        if (user != null) {
            telegramBotService.prepareAndSendMessage(chatId, userService.formatUserData(user));
        } else {
            telegramBotService.prepareAndSendMessage(chatId, "Пользователь не найден");
        }
    }

    private void handleDeleteDataCommand(long chatId, Message message) {
        boolean isSuccess = userService.deleteUser(message);
        if (isSuccess) {
            telegramBotService.prepareAndSendMessage(chatId, "✅ Данные успешно удалены");
        } else {
            telegramBotService.prepareAndSendMessage(chatId, "❌ Ошибка удаления данных");
        }
    }

    private void handleUsageCommand(long chatId, Long userId) {
        String usageInfo = rateLimitService.getUsageInfo(userId);
        telegramBotService.prepareAndSendMessage(chatId, usageInfo);
    }

    private void showCurrentTime(long chatId) {
        LocalDateTime localDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        String formattedTime = localDateTime.format(formatter);
        telegramBotService.prepareAndSendMessage(chatId, "⏰ Текущее время: " + formattedTime);
    }

    public void getRandomJoke(Long chatId) {
        try {
            String joke = jokerService.getJoke();
            telegramBotService.prepareAndSendMessage(chatId, "😂 " + joke);
            log.info("Joke sent to user: {}", chatId);
        } catch (JokeNotFoundException e) {
            telegramBotService.prepareAndSendMessage(chatId, "😅 Не удалось получить шутку. Попробуйте ещё раз!");
        } catch (Exception e) {
            log.error("Error getting random joke: {}", e.getMessage(), e);
            telegramBotService.prepareAndSendMessage(chatId, "⚠️ Извините, не удалось получить шутку. Попробуйте позже.");
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
        telegramBotService.prepareAndSendMessage(chatId, newsHelp);
    }

    public void handleTopNewsCommand(long chatId, String messageText) {
        String[] parts = messageText.split(" ");

        try {
            if (parts.length == 1) {
                telegramBotService.prepareAndSendMessage(chatId, "📡 Получаю главные новости USA...");
                String news = newsApiService.getTopHeadlinesForCountry("us", 5);
                telegramBotService.prepareAndSendMessage(chatId, news);
            } else if (parts.length == 2) {
                String country = parts[1];
                telegramBotService.prepareAndSendMessage(chatId, "📡 Получаю главные новости для " + country + "...");
                String news = newsApiService.getTopHeadlinesForCountry(country, 5);
                telegramBotService.prepareAndSendMessage(chatId, news);
            } else if (parts.length >= 3) {
                String country = parts[1];
                String category = String.join(" ", Arrays.copyOfRange(parts, 2, parts.length));
                telegramBotService.prepareAndSendMessage(chatId, "📡 Получаю новости категории '" + category + "' для " + country + "...");
                String news = newsApiService.getTopHeadlinesForCountryAndCategory(country, category, 5);
                telegramBotService.prepareAndSendMessage(chatId, news);
            }
        } catch (Exception e) {
            log.error("Error handling top news command: {}", e.getMessage(), e);
            telegramBotService.prepareAndSendMessage(chatId, "⚠️ Ошибка при получении новостей. Попробуйте позже.");
        }
    }

    public void handleNewsCategoryCommand(long chatId, String messageText) {
        String[] parts = messageText.split(" ");

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
                    """;
            telegramBotService.prepareAndSendMessage(chatId, categories);
        } else {
            try {
                String category = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                telegramBotService.prepareAndSendMessage(chatId, "📡 Получаю новости категории '" + category + "'...");
                String news = newsApiService.getTopHeadlinesForCategory(category, 5);
                telegramBotService.prepareAndSendMessage(chatId, news);
            } catch (Exception e) {
                log.error("Error handling news category command: {}", e.getMessage(), e);
                telegramBotService.prepareAndSendMessage(chatId, "⚠️ Ошибка при получении новостей. Проверьте название категории.");
            }
        }
    }

    public void handleNewsCountryCommand(long chatId, String messageText) {
        String[] parts = messageText.split(" ");

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
                    """;
            telegramBotService.prepareAndSendMessage(chatId, countries);
        } else {
            try {
                String country = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                telegramBotService.prepareAndSendMessage(chatId, "📡 Получаю новости для " + country + "...");
                String news = newsApiService.getTopHeadlinesForCountry(country, 5);
                telegramBotService.prepareAndSendMessage(chatId, news);
            } catch (Exception e) {
                log.error("Error handling news country command: {}", e.getMessage(), e);
                telegramBotService.prepareAndSendMessage(chatId, "⚠️ Ошибка при получении новостей. Проверьте название страны.");
            }
        }
    }

    public void handleNewsSearchCommand(long chatId, String messageText) {
        String[] parts = messageText.split(" ");

        if (parts.length == 1) {
            telegramBotService.prepareAndSendMessage(chatId,
                    "🔍 *Поиск новостей*\n\n" +
                    "*Использование:* /news_search [запрос]\n" +
                    "*Пример:* /news_search искусственный интеллект\n\n" +
                    "Я найду самые свежие новости по вашему запросу.");
        } else {
            try {
                String query = String.join(" ", Arrays.copyOfRange(parts, 1, parts.length));
                telegramBotService.prepareAndSendMessage(chatId, "🔍 Ищу новости по запросу: " + query + "...");
                String news = newsApiService.searchNews(query, 5);
                telegramBotService.prepareAndSendMessage(chatId, news);
            } catch (Exception e) {
                log.error("Error handling news search command: {}", e.getMessage(), e);
                telegramBotService.prepareAndSendMessage(chatId, "⚠️ Ошибка при поиске новостей. Попробуйте другой запрос.");
            }
        }
    }

    public void handleAiRequest(long chatId, Long userId, String messageText) {
        String question = extractQuestion(messageText);

        if (question.isEmpty()) {
            telegramBotService.prepareAndSendMessage(chatId, "❓ Пожалуйста, введите ваш вопрос");
            return;
        }

        if (!rateLimitService.canMakeAiRequest(userId)) {
            telegramBotService.prepareAndSendMessage(chatId,
                    "❌ Лимит AI-запросов исчерпан (5/день). Попробуйте завтра!\n" +
                    "Используйте /usage для проверки лимитов");
            return;
        }

        int remaining = rateLimitService.getRemainingAiRequests(userId);

        try {
            SendMessage thinkingMsg = new SendMessage();
            thinkingMsg.setChatId(String.valueOf(chatId));
            thinkingMsg.setText("🤔 Думаю над ответом... (осталось AI запросов: " + remaining + ")");
            telegramBotService.execute(thinkingMsg);

            String response = openRouterService.generateResponse(question);
            telegramBotService.prepareAndSendMessage(chatId, response);

            log.info("AI response generated for user {} (remaining: {})", userId, remaining - 1);

        } catch (Exception e) {
            log.error("AI request error for user {}: {}", userId, e.getMessage(), e);
            telegramBotService.prepareAndSendMessage(chatId, "⚠️ Ошибка при обращении к AI. Попробуйте позже.");
        }
    }

    public void handleCreditsCommand(long chatId) {
        if (config.getBotOwner().equals(chatId)) {
            String creditsInfo = openRouterLimitService.getUsageInfo();
            telegramBotService.prepareAndSendMessage(chatId, creditsInfo);
        } else {
            telegramBotService.prepareAndSendMessage(chatId, "❌ Эта команда только для владельца бота");
        }
    }

    public void startCommandReceived(long chatId, String username) {
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
        telegramBotService.sendMessageWithKeyboard(chatId, answer);
    }

    private String extractQuestion(String messageText) {
        if (messageText.startsWith("/ai")) {
            return messageText.length() > 4 ? messageText.substring(4).trim() : "";
        }
        return messageText.trim();
    }
}
