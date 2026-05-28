package com.example.book_talker_backend.monitoring;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ApiUsageReportScheduler {

    private final ApiUsageCounter counter;
    private final DataSource dataSource;

    @Value("${app.discord.webhook-url:}")
    private String webhookUrl;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(3))
            .build();

    @Scheduled(cron = "0 59 23 * * ?")
    public void sendDailyReport() {
        try {
            String message = buildReportMessage();
            sendToDiscord(message);
        } catch (Exception e) {
            log.error("일일 API 리포트 전송 실패", e);
        } finally {
            counter.clear();
        }
    }

    String buildReportMessage() {
        String date = LocalDate.now(ZoneId.of("Asia/Seoul")).toString();
        StringBuilder sb = new StringBuilder();

        sb.append("📊 BookTalker 일일 API 리포트 (").append(date).append(")\n\n");

        List<Map.Entry<String, Long>> top3 = counter.getTopN(3);
        sb.append("🔥 API 호출 Top 3\n");
        if (top3.isEmpty()) {
            sb.append("(호출 없음)\n");
        } else {
            String[] ranks = {"1위", "2위", "3위"};
            for (int i = 0; i < top3.size(); i++) {
                Map.Entry<String, Long> entry = top3.get(i);
                sb.append(ranks[i]).append("  ").append(entry.getKey())
                  .append("  ·  ").append(entry.getValue()).append("회\n");
            }
        }

        sb.append("\n");

        MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
        long usedMb = mem.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long maxMb = mem.getHeapMemoryUsage().getMax() / (1024 * 1024);
        int heapPct = maxMb > 0 ? (int) (usedMb * 100 / maxMb) : 0;
        sb.append("🧠 JVM Heap\n");
        sb.append("사용  ").append(usedMb).append(" MB  /  최대  ").append(maxMb)
          .append(" MB  (").append(heapPct).append("%)\n\n");

        if (dataSource instanceof HikariDataSource hikari) {
            var pool = hikari.getHikariPoolMXBean();
            if (pool != null) {
                sb.append("🔌 DB 커넥션 풀 (HikariCP)\n");
                sb.append("활성  ").append(pool.getActiveConnections())
                  .append("  /  유휴  ").append(pool.getIdleConnections())
                  .append("  /  대기  ").append(pool.getThreadsAwaitingConnection());
            }
        }

        return sb.toString();
    }

    private void sendToDiscord(String message) throws Exception {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            log.warn("Discord webhook URL이 설정되지 않아 리포트를 전송하지 않습니다.");
            return;
        }
        String payload = "{\"content\":\"" + escapeJson(message) + "\"}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .timeout(Duration.ofSeconds(5))
                .build();
        httpClient.send(request, HttpResponse.BodyHandlers.discarding());
    }

    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}
