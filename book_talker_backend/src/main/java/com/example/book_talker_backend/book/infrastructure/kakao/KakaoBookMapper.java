package com.example.book_talker_backend.book.infrastructure.kakao;

import com.example.book_talker_backend.book.entity.Book;
import com.example.book_talker_backend.book.infrastructure.BookSearchItem;
import com.example.book_talker_backend.book.infrastructure.BookSearchResult;
import com.example.book_talker_backend.book.infrastructure.IsbnParser;

import java.util.List;
import java.util.Optional;

public class KakaoBookMapper {
    public static BookSearchResult toResult(KakaoBookResponse kakaoBookResponse, int page) {
        List<BookSearchItem> items = kakaoBookResponse.documents().stream()
                .map(KakaoBookMapper::toItem)
                .flatMap(Optional::stream)
                .toList()
                ;

        return new BookSearchResult(
                items,
                kakaoBookResponse.meta().pageableCount(),
                page,
                !kakaoBookResponse.meta().isEnd() // is_end의 반대 = 다음 페이지 있음
        );
    }

    public static Optional<BookSearchItem> toItem(KakaoBookResponse.Documents kakaoBook) {
        return IsbnParser.toIsbn13(kakaoBook.isbn())
                .map(isbn13 -> new BookSearchItem(
                        isbn13,
                        kakaoBook.title(),
                        String.join(", ", kakaoBook.authors()),
                        kakaoBook.publisher(),
                        kakaoBook.thumbnail()
                )
        );
    }

    public static Book toDomain(BookSearchItem bookSearchItem) {
        return new Book(
                bookSearchItem.isbn13(),
                bookSearchItem.title(),
                bookSearchItem.author(),
                "미분류",
                bookSearchItem.publisher(),
                bookSearchItem.cover()
        );
    }
}
