package com.example.book_talker_backend.book.service;

import com.example.book_talker_backend.book.cache.SearchCacheKey;
import com.example.book_talker_backend.book.dao.BookRepository;
import com.example.book_talker_backend.book.entity.Book;
import com.example.book_talker_backend.book.entity.dto.SearchRequest;
import com.example.book_talker_backend.book.infrastructure.BookSearchClient;
import com.example.book_talker_backend.book.infrastructure.BookSearchItem;
import com.example.book_talker_backend.book.infrastructure.BookSearchResult;
import com.example.book_talker_backend.book.infrastructure.kakao.KakaoBookMapper;
import com.github.benmanes.caffeine.cache.Cache;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookService {
    private final BookSearchClient bookSearchClient;
    private final Cache<SearchCacheKey, BookSearchResult> searchCache;
    private final BookRepository bookRepository;

    public BookSearchResult search(SearchRequest request) {
        SearchCacheKey queryKey = new SearchCacheKey(request.query(), request.start());
        return searchCache.get(queryKey, key -> bookSearchClient.search(request.query(), request.start()));
    }

    public Book getBookByIsbn13WithApi(String isbn13) {
        BookSearchItem item = bookSearchClient.findByIsbn13(isbn13).orElseThrow(() -> new IllegalArgumentException("[BookTalker] Book with ISBN13 " + isbn13 + " does not exist"));

        return KakaoBookMapper.toDomain(item);
    }

    public Book getBookByIsbn13(String isbn13) {
        return bookRepository.findById(isbn13).orElse(null);
    }

    public void insertBook(Book book) {
        bookRepository.save(book);
    }
}
