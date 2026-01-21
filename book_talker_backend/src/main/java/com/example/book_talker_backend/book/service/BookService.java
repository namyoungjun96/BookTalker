package com.example.book_talker_backend.book.service;

import com.example.book_talker_backend.book.dao.BookRepository;
import com.example.book_talker_backend.book.entity.Book;
import com.example.book_talker_backend.book.entity.dto.AladinResponse;
import com.example.book_talker_backend.book.entity.dto.ListRequest;
import com.example.book_talker_backend.book.entity.dto.SearchRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class BookService {
    private final RestTemplate restTemplate;
    private final BookRepository bookRepository;

    private final static String ALADIN_BASE_URL = "http://www.aladin.co.kr/ttb/api";

    public AladinResponse list(ListRequest request) {
        String url = UriComponentsBuilder.fromUriString(ALADIN_BASE_URL + "/ItemList.aspx")
                .queryParam("TTBKey", "ttbehdgornltls1927001")
                .queryParam("QueryType", request.queryType().getValue())
                .queryParam("SearchTarget", "Book")
                .queryParam("Start", request.start())
                .queryParam("MaxResults", request.maxResults())
                .queryParam("cover", request.cover())
                .queryParam("Output", "js")
                .queryParam("Version", "20131101")
                .toUriString();

        return restTemplate.getForObject(url, AladinResponse.class);
    }

    public AladinResponse search(SearchRequest request) {
        String url = UriComponentsBuilder
                .fromUriString(ALADIN_BASE_URL + "/ItemSearch.aspx")
                .queryParam("TTBKey", "ttbehdgornltls1927001")
                .queryParam("Query", request.query())
                .queryParam("QueryType", "Title")
                .queryParam("SearchTarget", "Book")
                .queryParam("Start", request.start())
                .queryParam("MaxResults", request.maxResults())
                .queryParam("Cover", request.cover())
                .queryParam("Output", "js")
                .queryParam("InputEncoding", "utf-8")
                .queryParam("Version", "20131101")
                .build()
                .toUriString();

        return restTemplate.getForObject(url, AladinResponse.class);
    }

    public int insertBook(Book book) {
        try {
            bookRepository.save(book);
        } catch (IllegalArgumentException | OptimisticLockingFailureException e) {
            return 0;
        }

        return 1;
    }
}
