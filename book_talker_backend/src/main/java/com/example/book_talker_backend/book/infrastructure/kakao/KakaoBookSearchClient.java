package com.example.book_talker_backend.book.infrastructure.kakao;

import com.example.book_talker_backend.book.exception.KakaoApiException;
import com.example.book_talker_backend.book.infrastructure.BookSearchClient;
import com.example.book_talker_backend.book.infrastructure.BookSearchItem;
import com.example.book_talker_backend.book.infrastructure.BookSearchResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Optional;

@Component
public class KakaoBookSearchClient implements BookSearchClient {
    private final RestTemplate restTemplate;

    public KakaoBookSearchClient(RestTemplateBuilder builder, @Value("${api.key.kakao}") String apiKey) {
        this.restTemplate = builder
                .defaultHeader(HttpHeaders.AUTHORIZATION, "KakaoAK " + apiKey)
                .build()
        ;
    }

    private final static String API_BASE_URL = "https://dapi.kakao.com/v3/search/book";
    private final static String SEARCH_TITLE_TARGET = "title";
    private final static String SEARCH_TITLE_ISBN = "isbn";
    private final static String SEARCH_SORT = "accuracy";

    @Override
    public BookSearchResult search(String query, int page) {
        Map<String, Object> params = Map.of(
                "query", query,
                "page", page,
                "target", SEARCH_TITLE_TARGET,
                "size", SEARCH_PAGE_SIZE,
                "sort", SEARCH_SORT
        );

        KakaoBookResponse response = getResponse(params);

        return KakaoBookMapper.toResult(response, page);
    }

    @Override
    public Optional<BookSearchItem> findByIsbn13(String isbn13) {
        Map<String, Object> params = Map.of(
                "query", isbn13,
                "page", 1,
                "target", SEARCH_TITLE_ISBN,
                "size", FIND_PAGE_SIZE,
                "sort", SEARCH_SORT
        );

        KakaoBookResponse response = getResponse(params);

        return response.documents().stream()
                .findFirst()
                .flatMap(KakaoBookMapper::toItem);
    }

    private KakaoBookResponse getResponse(Map<String, Object> params) {
        String urlTemplate = API_BASE_URL + "?query={query}&target={target}&page={page}&size={size}&sort={sort}";

        KakaoBookResponse response = restTemplate.getForObject(urlTemplate, KakaoBookResponse.class, params);

        if(response == null || response.documents() == null) {
            throw new KakaoApiException("response is null");
        }

        return response;
    }
}
