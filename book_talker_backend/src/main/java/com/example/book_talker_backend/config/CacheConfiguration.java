package com.example.book_talker_backend.config;

import com.example.book_talker_backend.book.cache.SearchCacheKey;
import com.example.book_talker_backend.book.entity.dto.AladinResponse;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class CacheConfiguration {
    @Bean
    public Cache<SearchCacheKey, AladinResponse> bookSearchCache(MeterRegistry meterRegistry) {
        Cache<SearchCacheKey, AladinResponse> cache =  Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(Duration.ofDays(1))
                .recordStats()
                .build()
                ;

        return CaffeineCacheMetrics.monitor(meterRegistry, cache, "bookSearch");
    }
}
