package com.example.book_talker_backend.book;

import com.example.book_talker_backend.book.entity.dto.AladinBook;
import com.example.book_talker_backend.book.entity.dto.AladinResponse;
import com.example.book_talker_backend.book.entity.dto.BookSearchResponse;
import com.example.book_talker_backend.book.entity.dto.ListRequest;
import com.example.book_talker_backend.book.entity.dto.SearchRequest;
import com.example.book_talker_backend.book.service.BookService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.http.HttpStatus;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/book")
public class BookController {
    private final BookService bookService;

    @GetMapping("/list")
    public ResponseEntity<List<AladinBook>> list(@Valid ListRequest request) {
        log.info("검색 유형: {}", request.queryType());
        AladinResponse response = bookService.list(request);

        if (response == null) {
            log.debug("[list] internal server error: {}", request);
            return ResponseEntity.internalServerError().build();
        } else if (response.item().isEmpty()) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(response.item());
    }

    @GetMapping("/search")
    public ResponseEntity<BookSearchResponse> search(@Valid SearchRequest request) {
        log.info("검색어: {}", request.query());
        AladinResponse response = bookService.search(request);

        if (response == null) {
            log.debug("[search] internal server error: {}", request);
            return ResponseEntity.internalServerError().build();
        } else if (response.item().isEmpty()) {
            return ResponseEntity.ok(new BookSearchResponse(
                    List.of(),
                    response.totalResults(),
                    response.startIndex()
            ));
        }

        log.info("검색 결과: {} 건", response.totalResults());
        return ResponseEntity.ok(new BookSearchResponse(
                response.item(),
                response.totalResults(),
                response.startIndex()
        ));
    }

    @PostMapping
    public ResponseEntity<Void> insertBook(@RequestBody AladinBook request) {
        log.info("insert books... (isbn13: {})", request.isbn13());
        bookService.cacheBook(request);
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
