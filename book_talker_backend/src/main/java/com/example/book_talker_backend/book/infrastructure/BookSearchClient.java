package com.example.book_talker_backend.book.infrastructure;

import java.util.Optional;

public interface BookSearchClient {
    long SEARCH_PAGE_SIZE = 10;
    long FIND_PAGE_SIZE = 1;

    BookSearchResult search(String query, int page);
    Optional<BookSearchItem> findByIsbn13(String isbn13);
}
