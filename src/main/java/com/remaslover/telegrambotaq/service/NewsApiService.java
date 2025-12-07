package com.remaslover.telegrambotaq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.remaslover.telegrambotaq.dto.ArticleDTO;
import com.remaslover.telegrambotaq.dto.NewsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Map;

@Service
public class NewsApiService {

    private static final Logger log = LoggerFactory.getLogger(NewsApiService.class);

    @Value("${NEWS_API_KEY}")
    private String apiKey;

    private static final Map<String, String> COUNTRY_CODES = Map.ofEntries(
            Map.entry("россия", "ru"),
            Map.entry("russia", "ru"),
            Map.entry("ru", "ru"),
            Map.entry("сша", "us"),
            Map.entry("usa", "us"),
            Map.entry("us", "us"),
            Map.entry("америка", "us"),
            Map.entry("англия", "gb"),
            Map.entry("великобритания", "gb"),
            Map.entry("britain", "gb"),
            Map.entry("gb", "gb"),
            Map.entry("германия", "de"),
            Map.entry("germany", "de"),
            Map.entry("de", "de"),
            Map.entry("франция", "fr"),
            Map.entry("france", "fr"),
            Map.entry("fr", "fr"),
            Map.entry("китай", "cn"),
            Map.entry("china", "cn"),
            Map.entry("cn", "cn"),
            Map.entry("украина", "ua"),
            Map.entry("ukraine", "ua"),
            Map.entry("ua", "ua")
    );

    private static final Map<String, String> CATEGORIES = Map.ofEntries(
            Map.entry("общее", "general"),
            Map.entry("general", "general"),
            Map.entry("бизнес", "business"),
            Map.entry("business", "business"),
            Map.entry("развлечения", "entertainment"),
            Map.entry("entertainment", "entertainment"),
            Map.entry("здоровье", "health"),
            Map.entry("health", "health"),
            Map.entry("наука", "science"),
            Map.entry("science", "science"),
            Map.entry("спорт", "sports"),
            Map.entry("sports", "sports"),
            Map.entry("технологии", "technology"),
            Map.entry("technology", "technology"),
            Map.entry("тех", "technology")
    );

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    public NewsApiService(ObjectMapper objectMapper, RestTemplate restTemplate) {
        this.objectMapper = objectMapper;
        this.restTemplate = restTemplate;
    }


    /**
     * Основной метод для получения новостей с заданными параметрами
     *
     * @param query    - ключевые слова для поиска (например: "technology", "apple", "россия")
     * @param pageSize - количество статей на странице (макс. 100)
     * @param sortBy   - сортировка: "relevancy", "popularity", "publishedAt"
     * @return NewsDTO объект с результатами или null в случае ошибки
     */
    public NewsDTO getEverything(String query, int pageSize, String sortBy) {
        try {
            log.info("Fetching news from News API with query {}, pageSize: {}, sortBy: {}", query, pageSize, sortBy);

            if (apiKey == null || apiKey.isEmpty()) {
                log.error("News API key is empty");

                return createEmptyResponse("❌ API key news API is not configured");
            }

            String url = buildNewsApiUrll(query, pageSize, sortBy);
            log.debug("Request url: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "TelegramBot/1.0");
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("✅ News API response received, status: {}", response.getStatusCode());

                String responseBody = response.getBody();
                NewsDTO newsDTO = objectMapper.readValue(responseBody, NewsDTO.class);

                log.debug("Total results: {}", newsDTO.getTotalResults());
                if (newsDTO.getTotalResults() != null) {
                    log.debug("Articles count: {}", newsDTO.getArticles().length);
                }

                return newsDTO;
            } else {
                log.error("❌ News API error: {} - {}", response.getStatusCode(), response.getBody());

                return createErrorResponse("Http error: " + response.getStatusCode());
            }
        } catch (Exception ex) {
            log.error("❌ Error fetching news from News API: {}", ex.getMessage(), ex);
            return createErrorResponse("Ошибка при получении новостей: " + ex.getMessage());


        }


    }

    /**
     * Построение URL для запроса к News API
     */
    private String buildNewsApiUrll(String query, int pageSize, String sortBy) {
        return UriComponentsBuilder.fromHttpUrl("https://newsapi.org/v2/everything")
                .queryParam("q", query)
                .queryParam("pageSize", Math.min(pageSize, 100))
                .queryParam("sortBy", sortBy)
                .queryParam("from", getYesterdayDate())
                .queryParam("language", "ru")
                .queryParam("apiKey", apiKey)
                .toUriString();
    }

    /**
     * Получение даты вчерашнего дня в формате YYYY-MM-DD
     * News API требует дату для параметра 'from'
     */
    private String getYesterdayDate() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return yesterday.format(formatter);
    }

    /**
     * Создание пустого ответа с сообщением об ошибке
     */
    private NewsDTO createEmptyResponse(String errorMessage) {
        NewsDTO newsDTO = new NewsDTO();
        newsDTO.setStatus("error");
        newsDTO.setTotalResults(0);
        newsDTO.setArticles(new ArticleDTO[0]);

        return newsDTO;
    }

    /**
     * Создание ответа с ошибкой
     */
    private NewsDTO createErrorResponse(String errorMessage) {
        NewsDTO newsDTO = new NewsDTO();
        newsDTO.setStatus("error");
        newsDTO.setTotalResults(0);
        newsDTO.setArticles(new ArticleDTO[0]);
        return newsDTO;
    }


    /**
     * Экранирование Markdown для Telegram
     */
    private String escapeMarkdown(String text) {
        if (text == null) return "";
        return text.replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[")
                .replace("]", "\\]")
                .replace("(", "\\(")
                .replace(")", "\\)")
                .replace("~", "\\~")
                .replace("`", "\\`")
                .replace(">", "\\>")
                .replace("#", "\\#")
                .replace("+", "\\+")
                .replace("-", "\\-")
                .replace("=", "\\=")
                .replace("|", "\\|")
                .replace("{", "\\{")
                .replace("}", "\\}")
                .replace(".", "\\.")
                .replace("!", "\\!");
    }

    /**
     * Основной метод для получения топ новостей
     *
     * @param country  - код страны (ru, us, gb и т.д.)
     * @param category - категория (business, entertainment, general, health, science, sports, technology)
     * @param pageSize - количество новостей (макс. 100)
     * @param query    - ключевые слова для поиска внутри топ новостей (опционально)
     * @return NewsDTO с топ новостями
     */
    public NewsDTO getTopHeadlines(String country, String category, Integer pageSize, String query) {
        try {
            log.info("Fetching top headlines - country: {}, category: {}, pageSize: {}, query: {}",
                    country, category, pageSize, query);

            if (apiKey == null || apiKey.isEmpty()) {
                log.error("News API key is not configured");
                return createEmptyResponse("❌ API ключ News API не настроен");
            }

            if (country == null || country.isEmpty()) {
                country = "us";
            }

            if (category == null || category.isEmpty()) {
                category = "general";
            }

            if (pageSize == null || pageSize <= 0) {
                pageSize = 10;
            }

            String url = buildTopHeadlinesUrl(country, category, pageSize, query);
            log.debug("Top headlines request URL: {}", url);

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "TelegramBot/1.0");
            headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                log.info("✅ Top headlines received, status: {}", response.getStatusCode());

                String responseBody = response.getBody();
                NewsDTO newsDTO = objectMapper.readValue(responseBody, NewsDTO.class);

                log.debug("Top headlines total results: {}", newsDTO.getTotalResults());
                if (newsDTO.getArticles() != null) {
                    log.debug("Top headlines articles count: {}", newsDTO.getArticles().length);
                }

                return newsDTO;

            } else {
                log.error("❌ Top headlines API error: {} - {}", response.getStatusCode(), response.getBody());
                return createErrorResponse("HTTP ошибка при получении топ новостей: " + response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("❌ Error fetching top headlines: {}", e.getMessage(), e);
            return createErrorResponse("Ошибка при получении топ новостей: " + e.getMessage());
        }
    }

    /**
     * Построение URL для топ новостей
     */
    private String buildTopHeadlinesUrl(String country, String category, int pageSize, String query) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://newsapi.org/v2/top-headlines")
                .queryParam("country", country.toLowerCase())
                .queryParam("category", category.toLowerCase())
                .queryParam("pageSize", Math.min(pageSize, 100))
                .queryParam("apiKey", apiKey);

        if (query != null && !query.trim().isEmpty()) {
            builder.queryParam("q", query.trim());
        }

        return builder.toUriString();
    }


    /**
     * Получение топ новостей по стране (для Telegram бота)
     */
    public String getTopHeadlinesForCountry(String countryName, int count) {
        String countryCode = normalizeCountry(countryName);

        NewsDTO news = getTopHeadlines(countryCode, "general", count, null);
        return formatTopHeadlinesResponse(news, countryName, "general");
    }

    /**
     * Получение топ новостей по категории (для Telegram бота)
     */
    public String getTopHeadlinesForCategory(String categoryName, int count) {
        String categoryCode = normalizeCategory(categoryName);

        NewsDTO news = getTopHeadlines("us", categoryCode, count, null);
        return formatTopHeadlinesResponse(news, "USA", categoryName);
    }

    /**
     * Получение топ новостей по стране и категории
     */
    public String getTopHeadlinesForCountryAndCategory(String countryName, String categoryName, int count) {
        String countryCode = normalizeCountry(countryName);
        String categoryCode = normalizeCategory(categoryName);

        NewsDTO news = getTopHeadlines(countryCode, categoryCode, count, null);
        return formatTopHeadlinesResponse(news, countryName, categoryName);
    }

    /**
     * Нормализация названия страны в код
     */
    private String normalizeCountry(String countryInput) {
        if (countryInput == null || countryInput.isEmpty()) {
            return "ru";
        }

        String normalized = countryInput.toLowerCase().trim();
        return COUNTRY_CODES.getOrDefault(normalized, "ru");
    }

    /**
     * Нормализация категории
     */
    private String normalizeCategory(String categoryInput) {
        if (categoryInput == null || categoryInput.isEmpty()) {
            return "general";
        }

        String normalized = categoryInput.toLowerCase().trim();
        return CATEGORIES.getOrDefault(normalized, "general");
    }

    /**
     * Форматирование топ новостей для Telegram
     */
    private String formatTopHeadlinesResponse(NewsDTO news, String country, String category) {
        if (news == null || news.getArticles() == null || news.getArticles().length == 0) {
            return "📰 Главные новости " + (category != null ? "в категории '" + category + "' " : "")
                   + "для " + country + " не найдены.";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("📰 *Главные новости");

        if (category != null && !category.equalsIgnoreCase("general")) {
            sb.append(": ").append(category);
        }

        sb.append("*\n");
        sb.append("📍 *Страна:* ").append(getCountryName(country)).append("\n\n");

        for (int i = 0; i < Math.min(news.getArticles().length, 5); i++) {
            ArticleDTO article = news.getArticles()[i];

            sb.append("• *").append(escapeMarkdown(article.getTitle())).append("*\n");

            if (article.getSource() != null && article.getSource().getName() != null) {
                sb.append("  Источник: ").append(article.getSource().getName()).append("\n");
            }

            if (article.getPublishedAt() != null) {
                String timeAgo = getTimeAgo(article.getPublishedAt());
                sb.append("  ").append(timeAgo).append("\n");
            }

            if (article.getUrl() != null) {
                sb.append("  [Читать](").append(article.getUrl()).append(")\n");
            }

            sb.append("\n");
        }

        sb.append("_Всего новостей: ").append(news.getTotalResults()).append("_");

        return sb.toString();
    }

    /**
     * Получение читаемого названия страны
     */
    private String getCountryName(String countryCode) {
        Map<String, String> countryNames = Map.of(
                "ru", "Россия 🇷🇺",
                "us", "США 🇺🇸",
                "gb", "Великобритания 🇬🇧",
                "de", "Германия 🇩🇪",
                "fr", "Франция 🇫🇷",
                "cn", "Китай 🇨🇳",
                "ua", "Украина 🇺🇦"
        );

        return countryNames.getOrDefault(countryCode.toLowerCase(), countryCode);
    }

    /**
     * Расчет времени с момента публикации
     */
    private String getTimeAgo(String isoDate) {
        try {
            java.time.Instant published = java.time.Instant.parse(isoDate);
            java.time.Instant now = java.time.Instant.now();

            long hoursAgo = java.time.Duration.between(published, now).toHours();

            if (hoursAgo < 1) {
                long minutesAgo = java.time.Duration.between(published, now).toMinutes();
                return minutesAgo + " мин. назад";
            } else if (hoursAgo < 24) {
                return hoursAgo + " ч. назад";
            } else {
                long daysAgo = hoursAgo / 24;
                return daysAgo + " дн. назад";
            }
        } catch (Exception e) {
            return "Сегодня";
        }
    }

    public String searchNews(String query, int count) {
        try {
            NewsDTO news = getEverything(query, count, "publishedAt");

            if (news == null || news.getArticles() == null || news.getArticles().length == 0) {
                return "🔍 Новости по запросу '" + query + "' не найдены.";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("🔍 *Результаты поиска: ").append(query).append("*\n\n");

            for (int i = 0; i < Math.min(news.getArticles().length, count); i++) {
                ArticleDTO article = news.getArticles()[i];

                sb.append(i + 1).append(". *").append(escapeMarkdown(article.getTitle())).append("*\n");

                if (article.getDescription() != null && !article.getDescription().isEmpty()) {
                    sb.append("   ").append(article.getDescription()).append("\n");
                }

                if (article.getSource() != null && article.getSource().getName() != null) {
                    sb.append("   Источник: ").append(article.getSource().getName()).append("\n");
                }

                if (article.getUrl() != null) {
                    sb.append("   [Читать](").append(article.getUrl()).append(")\n");
                }

                sb.append("\n");
            }

            sb.append("_Всего найдено: ").append(news.getTotalResults()).append(" статей_");

            return sb.toString();

        } catch (Exception e) {
            log.error("Error searching news: {}", e.getMessage(), e);
            return "⚠️ Ошибка при поиске новостей. Попробуйте позже.";
        }
    }
}
