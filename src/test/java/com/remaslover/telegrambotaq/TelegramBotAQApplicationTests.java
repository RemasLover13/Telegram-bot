package com.remaslover.telegrambotaq;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class TelegramBotAQApplicationTests {


    @Autowired
    ApplicationContext applicationContext;


    @Test
    void contextLoads() {
        assert (applicationContext != null);
    }

}
