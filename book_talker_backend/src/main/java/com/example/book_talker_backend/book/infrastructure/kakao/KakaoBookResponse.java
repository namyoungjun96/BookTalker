package com.example.book_talker_backend.book.infrastructure.kakao;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record KakaoBookResponse (
        List<Documents> documents,
        Meta meta
) {
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record Documents (
            List<String> authors,
            String contents,
            String isbn,
            String publisher,
            String thumbnail,
            String title
//            String datetime,
//            double price,
//            double salePrice,
//            String status,
//            String translators,
//            String url
            // 사용하지 않는 파라미터는 주석 표기 (역직렬화가 깨지는 것을 방지)
    ) {
    }

    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    record Meta (
            boolean isEnd,
            long pageableCount,
            long totalCount
    ) {
    }
}
