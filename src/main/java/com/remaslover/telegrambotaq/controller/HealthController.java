package com.remaslover.telegrambotaq.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/ping")
    public Map<String, Object> ping() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("service", "Telegram Bot");
        response.put("timestamp", LocalDateTime.now().toString());
        return response;
    }

    @GetMapping("/")
    public String home() {
        String videoId = "Sar0sxF8Umc";
        String currentTime = LocalDateTime.now().toString();

        return String.format("""
            <!DOCTYPE html>
            <html lang="ru">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>🤖 Telegram AI Bot</title>
               \s
                <!-- ФАВИКОН -->
                <link rel="icon" href="/assets/images/favicon.ico" type="image/x-icon">
                <link rel="shortcut icon" href="/assets/images/favicon.ico" type="image/x-icon">
               \s
                <!-- Мета-теги -->
                <meta name="description" content="Telegram AI Assistant - умный бот с поддержкой контекста">
                <meta name="keywords" content="telegram, bot, ai, spring boot, openrouter">
               \s
                <!-- Open Graph для соцсетей -->
                <meta property="og:title" content="Telegram AI Assistant">
                <meta property="og:description" content="Умный бот с поддержкой контекста и многократного диалога">
                <meta property="og:image" content="/assets/images/logo.png">
                <meta property="og:image:width" content="640">
                <meta property="og:image:height" content="640">
                <meta property="og:url" content="https://ваш-домен.com/">
                <meta property="og:type" content="website">
               \s
                <!-- Twitter Card -->
                <meta name="twitter:card" content="summary_large_image">
                <meta name="twitter:title" content="Telegram AI Assistant">
                <meta name="twitter:description" content="Умный бот с поддержкой контекста">
                <meta name="twitter:image" content="/assets/images/logo.png">
               \s
                <link rel="stylesheet" href="/css/styles.css">
            </head>
            <body>
                <div class="container">
                    <!-- ЗАГОЛОВОК С ЛОГОТИПОМ -->
                    <div class="header">
                        <div class="logo-container">
                            <img src="/assets/images/logo.png"\s
                                 alt="AI Bot Logo"\s
                                 class="logo"
                                 width="120"
                                 height="120">
                            <div class="logo-text">
                                <h1>🤖 Telegram AI Assistant</h1>
                                <p class="subtitle">Умный бот с поддержкой контекста и многократного диалога</p>
                            </div>
                        </div>
                       \s
                        <div class="status-badge">Status: <strong>RUNNING</strong></div>
                        <p class="server-time">Server Time: %s</p>
                    </div>
                   \s
                    <!-- МЕДИА КОНТЕЙНЕР -->
                    <div class="media-container">
                        <!-- Видео с YouTube -->
                        <div class="video-wrapper">
                            <h3>🎥 Демонстрация работы бота</h3>
                            <iframe\s
                                src="https://www.youtube.com/embed/%s?controls=1&rel=0&modestbranding=1"\s
                                title="YouTube video player"\s
                                allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture"\s
                                allowfullscreen>
                            </iframe>
                            <p><small>Обзор функционала Telegram AI бота</small></p>
                           \s
                            <!-- Превью изображение под видео -->
                            <img src="/assets/images/bot-preview.jpg"\s
                                 alt="Bot Interface Preview"\s
                                 class="preview-image">
                        </div>
                       \s
                        <!-- Аудиоплеер -->
                        <div class="audio-wrapper">
                            <h3>🎵 Музыкальный плеер</h3>
                            <p>Выберите трек для прослушивания</p>
                           \s
                            <audio id="mainAudioPlayer" controls class="audio-player">
                                Ваш браузер не поддерживает аудио элемент.
                            </audio>
                           \s
                            <div class="controls">
                                <button class="control-btn" onclick="playTrack('background-music')">▶️ Play</button>
                                <button class="control-btn" onclick="document.getElementById('mainAudioPlayer').pause()">⏸️ Pause</button>
                                <button class="control-btn" onclick="changeVolume(-0.1)">🔉 -</button>
                                <button class="control-btn" onclick="changeVolume(0.1)">🔊 +</button>
                                <button class="control-btn" onclick="nextTrack()">⏭️ Next</button>
                            </div>
                           \s
                            <div class="playlist">
                                <div class="track-item active" onclick="playTrack('background-music')">
                                    <span class="track-number">1</span>
                                    <span class="track-emoji">🎵</span>
                                    Расслабляющий саундтрек (2:30)
                                </div>
                                <div class="track-item" onclick="playTrack('track1')">
                                    <span class="track-number">2</span>
                                    <span class="track-emoji">⚡</span>
                                    Энергичная фоновая музыка (3:15)
                                </div>
                                <div class="track-item" onclick="playTrack('track2')">
                                    <span class="track-number">3</span>
                                    <span class="track-emoji">🚀</span>
                                    Космическая атмосфера (4:20)
                                </div>
                            </div>
                           \s
                            <p><small>Формат: MP3 | Автопереключение треков</small></p>
                        </div>
                    </div>
                   \s
                    <!-- ФУНКЦИИ -->
                    <div class="features">
                        <div class="feature">
                            <div class="feature-icon">🌍</div>
                            <h4>🚀 Основные функции</h4>
                            <p>• Поддержка 50+ языков<br>• Контекст диалога<br>• Markdown форматирование<br>• Многопользовательский режим</p>
                        </div>
                        <div class="feature">
                            <div class="feature-icon">📊</div>
                            <h4>📊 Статистика</h4>
                            <p>• Аптайм: 99.9%%<br>• Ответ за &lt;2 секунды<br>• Безлимитные запросы<br>• 24/7 доступность</p>
                        </div>
                        <div class="feature">
                            <div class="feature-icon">⚙️</div>
                            <h4>🔧 API & Интеграции</h4>
                            <p>• <a href="/ping">/ping</a> - проверка работы<br>• <a href="/health">/health</a> - детальный статус<br>• OpenRouter AI<br>• Spring Boot</p>
                        </div>
                    </div>
                   \s
                    <!-- ФУТЕР -->
                    <footer>
                        <div class="footer-logo">
                            <img src="/assets/images/logo.png"\s
                                 alt="Logo"\s
                                 width="50"\s
                                 height="50"
                                 style="border-radius: 10px; margin-right: 10px;">
                            <span>© 2024 AI Telegram Bot | Powered by Spring Boot & OpenRouter AI</span>
                        </div>
                        <p style="margin-top: 10px; font-size: 0.8em;">
                            Видео: YouTube | Музыка: Royalty Free Tracks | Logo: Custom Design
                        </p>
                    </footer>
                </div>
               \s
                <script src="/js/player.js"></script>
                <script>
                    // Инициализация
                    document.addEventListener('DOMContentLoaded', function() {
                        playTrack('background-music');
                    });
                </script>
            </body>
            </html>
           \s""", currentTime, videoId);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("version", "2.0");
        health.put("keepAlive", "enabled");
        health.put("media", "enabled");
        health.put("youtube_video", "embedded");
        health.put("audio_playlist", "3 tracks");
        return health;
    }
}
