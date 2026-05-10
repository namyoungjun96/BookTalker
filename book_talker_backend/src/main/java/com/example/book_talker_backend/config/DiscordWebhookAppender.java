package com.example.book_talker_backend.config;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DiscordWebhookAppender extends AppenderBase<ILoggingEvent> {

    private String webhookUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                    .withZone(ZoneId.of("Asia/Seoul"));

    @Override
    protected void append(ILoggingEvent event) {
        if (webhookUrl == null || webhookUrl.isBlank()) return;
        try {
            String timestamp = FORMATTER.format(Instant.ofEpochMilli(event.getTimeStamp()));
            String content = String.format("[ERROR] %s - %s - %s",
                    timestamp, event.getLoggerName(), event.getFormattedMessage());
            String payload = "{\"content\":\"" + escapeJson(content) + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .timeout(Duration.ofSeconds(5))
                    .build();

            httpClient.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            addError("Discord webhook 전송 실패", e);
        }
    }

    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }

    public void setWebhookUrl(String webhookUrl) {
        this.webhookUrl = webhookUrl;
    }
}
