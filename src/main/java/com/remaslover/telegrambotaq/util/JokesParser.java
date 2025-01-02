package com.remaslover.telegrambotaq.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JokesParser {

    private static final Logger log = LoggerFactory.getLogger(JokesParser.class);

    public static String getJokeFromSites() {
        try {
            Document document = Jsoup.connect("https://www.anekdot.ru/random/anekdot/").get();

            String joke = document.select("div.text").first().text();

            return joke;
        } catch (Exception e) {
            log.error("Ошибка при получении анекдота: {}", e.getMessage());
            return "Не удалось получить анекдот";
        }
    }
}
