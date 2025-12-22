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
        String videoId = "https://www.youtube.com/watch?v=Sar0sxF8Umc";

        return """
                <html>
                <head>
                    <title>🤖 Telegram Bot</title>
                    <style>
                        body {
                            font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                            margin: 0;
                            padding: 20px;
                            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
                            color: white;
                            min-height: 100vh;
                            text-align: center;
                        }
                        .container {
                            max-width: 1200px;
                            margin: 0 auto;
                            background: rgba(0, 0, 0, 0.7);
                            padding: 30px;
                            border-radius: 20px;
                            backdrop-filter: blur(10px);
                            box-shadow: 0 10px 30px rgba(0,0,0,0.3);
                        }
                        .media-container {
                            display: flex;
                            flex-wrap: wrap;
                            justify-content: center;
                            gap: 30px;
                            margin: 40px 0;
                        }
                        .video-wrapper, .audio-wrapper {
                            flex: 1;
                            min-width: 300px;
                            background: rgba(255, 255, 255, 0.1);
                            padding: 20px;
                            border-radius: 15px;
                        }
                        iframe {
                            border-radius: 10px;
                            box-shadow: 0 5px 15px rgba(0,0,0,0.2);
                            width: 100%;
                            height: 315px;
                        }
                        audio {
                            width: 100%;
                            margin-top: 15px;
                        }
                        .status-badge {
                            display: inline-block;
                            background: #4CAF50;
                            padding: 8px 20px;
                            border-radius: 20px;
                            font-weight: bold;
                            margin: 10px;
                        }
                        h1 {
                            font-size: 3em;
                            margin-bottom: 10px;
                        }
                        .features {
                            display: flex;
                            justify-content: center;
                            gap: 20px;
                            margin: 30px 0;
                            flex-wrap: wrap;
                        }
                        .feature {
                            background: rgba(255,255,255,0.1);
                            padding: 15px;
                            border-radius: 10px;
                            min-width: 200px;
                        }
                    </style>
                </head>
                <body>
                    <div class="container">
                        <h1>🤖 Telegram AI Assistant</h1>
                        <p>Умный бот с поддержкой контекста и многократного контекста</p>
                        
                        <div class="status-badge">Status: <strong>RUNNING</strong></div>
                        <p>Server Time: %s</p>
                        
                        <div class="media-container">
                            <!-- Видео с YouTube -->
                            <div class="video-wrapper">
                                <h3>🎥 Демонстрация работы бота</h3>
                                <iframe src="https://www.youtube.com/embed/%s?controls=1&rel=0" 
                                        title="YouTube video player" 
                                        frameborder="0" 
                                        allow="accelerometer; autoplay; clipboard-write; encrypted-media; gyroscope; picture-in-picture" 
                                        allowfullscreen>
                                </iframe>
                            </div>
                            
                            <!-- Аудиоплеер -->
                            <div class="audio-wrapper">
                                <h3>🎵 Фоновая музыка</h3>
                                <p>Расслабляющий саундтрек для работы</p>
                                <audio controls>
                                    <source src="/assets/background-music.mp3" type="audio/mpeg">
                                    Ваш браузер не поддерживает аудио элемент.
                                </audio>
                                <p><small>Формат: MP3 | Время: 2:30</small></p>
                            </div>
                        </div>
                        
                        <div class="features">
                            <div class="feature">
                                <h4>🚀 Функции</h4>
                                <p>• Поддержка 50+ языков<br>• Контекст диалога<br>• Markdown форматирование</p>
                            </div>
                            <div class="feature">
                                <h4>📊 Статистика</h4>
                                <p>• Аптайм: 99.9%<br>• Ответ за &lt;2с<br>• Безлимитные запросы</p>
                            </div>
                            <div class="feature">
                                <h4>🔧 API</h4>
                                <p>• <a href="/ping" style="color:#4fc3f7;">/ping</a> - проверка работы<br>• <a href="/health" style="color:#4fc3f7;">/health</a> - детальный статус</p>
                            </div>
                        </div>
                        
                        <footer>
                            <p>© 2024 AI Telegram Bot | Powered by Spring Boot & OpenRouter AI</p>
                        </footer>
                    </div>
                </body>
                </html>
                """.formatted(LocalDateTime.now(), videoId);
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("version", "1.0");
        health.put("keepAlive", "enabled");
        return health;
    }
}
