package com.remaslover.telegrambotaq.job;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@EnableAsync
public class AsyncSelfPinger {

    private static final Logger log = LoggerFactory.getLogger(AsyncSelfPinger.class);

    @Value("${RENDER_APP_URL}")
    private String appUrl;
    private final RestTemplate restTemplate;
    private final AtomicInteger pingCount = new AtomicInteger(0);

    public AsyncSelfPinger() {
        this.restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory simpleClientHttpRequestFactory = new SimpleClientHttpRequestFactory();
        simpleClientHttpRequestFactory.setConnectTimeout(3000);
        simpleClientHttpRequestFactory.setReadTimeout(3000);
        restTemplate.setRequestFactory(simpleClientHttpRequestFactory);
    }

    @PostConstruct
    public void init() {
        log.info("🚀 AsyncSelfPinger инициализирован. Будем пинговать: {}", appUrl);
        pingSelf();
    }

    @Async
    @Scheduled(fixedRate = 8 * 60 * 1000)
    public void pingSelf() {
        int attempt = pingCount.incrementAndGet();
        try {
            long startTime = System.currentTimeMillis();
            String response = restTemplate.getForObject(appUrl + "/ping", String.class);
            long duration = System.currentTimeMillis() - startTime;

            if ("pong".equalsIgnoreCase(response)) {
                log.debug("✅ Пинг #{} успешен ({} мс)", attempt, duration);
            } else {
                log.warn("⚠️ Пинг #{}: неожиданный ответ: {}", attempt, response);
            }
        } catch (Exception e) {
            log.info("🔴 Пинг #{} не удался: {}", attempt, e.getMessage());
        }
    }
}
