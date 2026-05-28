package com.example.book_talker_backend.monitoring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

@Component
@RequiredArgsConstructor
public class ApiUsageInterceptor implements HandlerInterceptor {

    private final ApiUsageCounter counter;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!(handler instanceof HandlerMethod)) return true;

        String pattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern == null) pattern = request.getRequestURI();

        counter.increment(request.getMethod() + " " + pattern);
        return true;
    }
}
