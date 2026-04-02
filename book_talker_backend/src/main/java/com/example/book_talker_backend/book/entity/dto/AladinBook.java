package com.example.book_talker_backend.book.entity.dto;

import java.time.LocalDate;

public record AladinBook(
        String title,
        String link,
        String author,
        LocalDate pubDate,
        String description,
        String isbn,
        String isbn13,
        String itemId,
        long priceSales,
        long priceStandard,
        String mallType,
        String stockStatus,
        long mileage,
        String cover,
        long categoryId,
        String categoryName,
        String publisher,
        long salesPoint,
        boolean adult,
        boolean fixedPrice,
        long customerReviewRank
//                subInfo
) {
}
