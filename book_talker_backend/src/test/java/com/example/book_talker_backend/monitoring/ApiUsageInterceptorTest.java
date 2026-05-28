package com.example.book_talker_backend.monitoring;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ApiUsageInterceptorTest {

    @Mock ApiUsageCounter counter;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock HandlerMethod handlerMethod;
    @InjectMocks ApiUsageInterceptor interceptor;

    @Test
    void preHandle_HandlerMethod일때_카운터_증가() {
        given(request.getMethod()).willReturn("GET");
        given(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)).willReturn("/rank");

        boolean result = interceptor.preHandle(request, response, handlerMethod);

        assertThat(result).isTrue();
        then(counter).should().increment("GET /rank");
    }

    @Test
    void preHandle_HandlerMethod_아닐때_스킵() {
        boolean result = interceptor.preHandle(request, response, "not-a-handler");

        assertThat(result).isTrue();
        then(counter).should(never()).increment(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void preHandle_패턴_없을때_URI_폴백() {
        given(request.getMethod()).willReturn("GET");
        given(request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE)).willReturn(null);
        given(request.getRequestURI()).willReturn("/rank");

        interceptor.preHandle(request, response, handlerMethod);

        then(counter).should().increment("GET /rank");
    }
}
