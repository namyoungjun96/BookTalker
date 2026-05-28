package com.example.book_talker_backend.monitoring;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Component
public class ApiUsageCounter {

    private final ConcurrentHashMap<String, AtomicLong> counts = new ConcurrentHashMap<>();

    public void increment(String key) {
        counts.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }

    public List<Map.Entry<String, Long>> getTopN(int n) {
        Map<String, Long> snapshot = new HashMap<>();
        counts.forEach((k, v) -> snapshot.put(k, v.get()));

        return snapshot.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(n)
                .collect(Collectors.toList());
    }

    public void clear() {
        counts.clear();
    }
}
