package com.remaslover.telegrambotaq;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class JokeFetchTest {
    @Test
    void testGetJokeFromSitesRealRequest() throws Exception {
        Document document = Jsoup.connect("https://www.anekdot.ru/random/anekdot/").get();

        String joke = document.select("div.text").first().text();
        System.out.println(joke);
        assertTrue(joke.length() > 0);
    }
}
