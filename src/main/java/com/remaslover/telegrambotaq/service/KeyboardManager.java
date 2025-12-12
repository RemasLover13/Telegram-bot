package com.remaslover.telegrambotaq.service;

import com.remaslover.telegrambotaq.enums.Button;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Component
public class KeyboardManager {

    public static class Button {
        public static final String YES_BUTTON = "BUTTON_YES";
        public static final String NO_BUTTON = "BUTTON_NO";

        public static final String CONTEXT_CLEAR = "/context clear";
        public static final String CONTEXT_SHOW = "/context show";
        public static final String CONTEXT_SHOW_DEBUG = "/context show_debug";
        public static final String CONTEXT_STATS = "/context stats";
        public static final String CONTEXT_HELP = "/context help";

        public static final String NEWS_TECH = "/news_category технологии";
        public static final String NEWS_SPORTS = "/news_category спорт";
        public static final String NEWS_RUSSIA = "/news_country россия";
        public static final String NEWS_USA = "/news_country сша";
    }


    public InlineKeyboardMarkup createContextKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createInlineButton("🧹 Очистить", Button.CONTEXT_CLEAR));
        row1.add(createInlineButton("👁️ Показать", Button.CONTEXT_SHOW));

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createInlineButton("🐛 Отладка", Button.CONTEXT_SHOW_DEBUG));
        row2.add(createInlineButton("📊 Статистика", Button.CONTEXT_STATS));

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createInlineButton("❓ Помощь", Button.CONTEXT_HELP));

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    /**
     * Создает кнопку с callback-данными
     */
    private InlineKeyboardButton createInlineButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }

    /**
     * Создает клавиатуру для быстрого доступа к новостям
     */
    public InlineKeyboardMarkup createNewsQuickKeyboard() {
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        List<InlineKeyboardButton> row1 = new ArrayList<>();
        row1.add(createInlineButton("🤖 Технологии", Button.NEWS_TECH));
        row1.add(createInlineButton("⚽ Спорт", Button.NEWS_SPORTS));

        List<InlineKeyboardButton> row2 = new ArrayList<>();
        row2.add(createInlineButton("🇷🇺 Россия", Button.NEWS_RUSSIA));
        row2.add(createInlineButton("🇺🇸 США", Button.NEWS_USA));

        List<InlineKeyboardButton> row3 = new ArrayList<>();
        row3.add(createInlineButton("🔥 Главные", "/topnews"));
        row3.add(createInlineButton("🔍 Поиск", "/news_search"));

        rows.add(row1);
        rows.add(row2);
        rows.add(row3);

        keyboard.setKeyboard(rows);
        return keyboard;
    }

    public ReplyKeyboardMarkup createMainKeyboard() {
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

        KeyboardRow row4 = new KeyboardRow();
        row4.add("🧠 Контекст");
        row4.add("🔍 Поиск новостей");
        keyboardRows.add(row4);

        keyboardMarkup.setKeyboard(keyboardRows);
        return keyboardMarkup;
    }

    public InlineKeyboardMarkup createRegistrationKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();

        var yesButton = new InlineKeyboardButton();
        yesButton.setText("✅ Да");
        yesButton.setCallbackData(Button.YES_BUTTON);

        var noButton = new InlineKeyboardButton();
        noButton.setText("❌ Нет");
        noButton.setCallbackData(Button.NO_BUTTON);

        rowInline.add(yesButton);
        rowInline.add(noButton);
        rows.add(rowInline);
        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    public List<BotCommand> createBotCommands() {
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "начать работу"));
        listOfCommands.add(new BotCommand("/help", "помощь"));
        listOfCommands.add(new BotCommand("/my_data", "мои данные"));
        listOfCommands.add(new BotCommand("/delete_data", "удалить данные"));
        listOfCommands.add(new BotCommand("/time", "текущее время"));
        listOfCommands.add(new BotCommand("/joke", "случайная шутка"));
        listOfCommands.add(new BotCommand("/ai", "задать вопрос AI"));
        listOfCommands.add(new BotCommand("/context", "управление контекстом"));
        listOfCommands.add(new BotCommand("/usage", "мои лимиты"));
        listOfCommands.add(new BotCommand("/credits", "остатки OpenRouter"));
        listOfCommands.add(new BotCommand("/topnews", "главные новости"));
        listOfCommands.add(new BotCommand("/news_category", "новости по категории"));
        listOfCommands.add(new BotCommand("/news_country", "новости по стране"));
        listOfCommands.add(new BotCommand("/news_search", "поиск новостей"));

        return listOfCommands;
    }


}
