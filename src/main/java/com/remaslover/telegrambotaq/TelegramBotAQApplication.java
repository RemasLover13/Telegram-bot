package com.remaslover.telegrambotaq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableTransactionManagement
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class TelegramBotAQApplication {

    public static void main(String[] args) {
        SpringApplication.run(TelegramBotAQApplication.class, args);
    }

}
