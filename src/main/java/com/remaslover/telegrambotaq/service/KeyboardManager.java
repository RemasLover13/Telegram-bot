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
        yesButton.setCallbackData(Button.YES_BUTTON.name());

        var noButton = new InlineKeyboardButton();
        noButton.setText("❌ Нет");
        noButton.setCallbackData(Button.NO_BUTTON.name());

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
