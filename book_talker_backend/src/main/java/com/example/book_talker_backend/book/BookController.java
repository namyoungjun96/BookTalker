package com.example.book_talker_backend.book;

import com.example.book_talker_backend.book.entity.dto.SearchRequest;
import com.example.book_talker_backend.book.infrastructure.BookSearchResult;
import com.example.book_talker_backend.book.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/book")
public class BookController {
    private final BookService bookService;

    @GetMapping("/search")
    public ResponseEntity<BookSearchResult> search(@Valid SearchRequest request) {
        log.info("검색어: {}", request.query());
        return ResponseEntity.ok(bookService.search(request));
    }
}
