package com.example.book_talker_backend.book;

import com.example.book_talker_backend.book.dao.BookRepository;
import com.example.book_talker_backend.book.entity.Book;
import com.example.book_talker_backend.book.entity.dto.AladinBook;
import com.example.book_talker_backend.book.entity.dto.AladinResponse;
import com.example.book_talker_backend.book.entity.dto.SearchRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/book")
public class BookController {
    private final BookRepository bookRepository;
    private final RestTemplate restTemplate;

    private final static String ALADIN_BASE_URL = "http://www.aladin.co.kr/ttb/api";

    @GetMapping
    public ResponseEntity<List<Book>> getAllBooks() {
        List<Book> bookList = bookRepository.findAll();

        return ResponseEntity.ok(bookList);
    }

    @GetMapping("/search")
    public ResponseEntity<List<AladinBook>> searchBookList(@Valid SearchRequest request) {
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

        log.info("검색어: {}", request.query());

        AladinResponse response = restTemplate.getForObject(url, AladinResponse.class);

        log.info("검색 결과: {} 건", response.totalResults());

        if (response == null) {
            return ResponseEntity.internalServerError().build();
        } else if (response.item().isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response.item());
    }

    @PostMapping
    public ResponseEntity<Void> crate(Book book) {
        bookRepository.save(book);

        return new ResponseEntity<>(HttpStatus.CREATED);
    }

    @PutMapping
    public ResponseEntity<Void> update(Book book) {
        bookRepository.save(book);

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@RequestBody Book book) {
        bookRepository.delete(book);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
