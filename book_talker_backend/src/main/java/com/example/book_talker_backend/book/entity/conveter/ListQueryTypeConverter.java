package com.example.book_talker_backend.book.entity.conveter;

import com.example.book_talker_backend.book.entity.dto.QueryType;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class ListQueryTypeConverter implements Converter<String, QueryType> {
    @Override
    public QueryType convert(String value) {
        return QueryType.of(value);
    }
}