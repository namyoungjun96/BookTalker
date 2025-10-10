package com.example.book_talker_backend.book.entity.dto;

import com.example.book_talker_backend.book.exception.UnknownEnumValueException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum QueryType {
    ITEM_NEW_ALL("ItemNewAll", "신간 전체 리스트"),
    ITEM_NEW_SPECIAL("ItemNewSpecial", "주목할 만한 신간 리스트"),
    ITEM_EDITOR_CHOICE("ItemEditorChoice", "편집자 추천 리스트(카테고리로만 조회 가능 - 국내도서/음반/외서만 지원)"),
    BEST_SELLER("Bestseller", "베스트셀러"),
    BLOG_BEST("BlogBest", "블로거 베스트셀러 (국내도서만 조회 가능)");

    private final String value;
    private final String description;

    QueryType(String value, String description) {
        this.value = value;
        this.description = description;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static QueryType of(String value) {
        if (null == value) {
            return null;
        }

        for (QueryType item : QueryType.values()) {
            if (value.equals(item.getValue())) {
                return item;
            }
        }

        throw new UnknownEnumValueException("QueryTypeEnum: unknown value: " + value);
    }
}