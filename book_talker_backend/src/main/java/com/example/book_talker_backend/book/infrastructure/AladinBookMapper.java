package com.example.book_talker_backend.book.infrastructure;

import com.example.book_talker_backend.book.entity.Book;
import com.example.book_talker_backend.book.entity.dto.AladinBook;

import java.util.Set;

public class AladinBookMapper {

    private static final Set<String> ETC_GENRES = Set.of(
            "건강/취미", "공무원 수험서", "달력/기타", "대학교재", "수험서/자격증",
            "어린이", "여행", "예술/대중문화", "외국어", "요리/살림", "유아",
            "잡지", "전집/중고전집", "좋은부모", "청소년", "컴퓨터/모바일",
            "초등학교참고서", "중학교참고서", "고등학교참고서"
    );

    public static Book toDomain(AladinBook aladinBook) {
        Book book = new Book();
        book.setIsbn13(aladinBook.isbn13());
        book.setTitle(aladinBook.title());
        book.setAuthor(aladinBook.author());
        book.setGenre(extractGenre(aladinBook.categoryName()));
        book.setPublisher(aladinBook.publisher());
        book.setCover(aladinBook.cover());
        return book;
    }

    private static String extractGenre(String categoryName) {
        if (categoryName == null || categoryName.isBlank()) return null;
        String[] parts = categoryName.split(">");
        String genre = parts.length >= 2 ? parts[1].trim() : parts[0].trim();
        return ETC_GENRES.contains(genre) ? "기타" : genre;
    }
}
