package com.example.book_talker_backend.book.entity.conveter;

import com.example.book_talker_backend.book.entity.dto.CoverSizeEnum;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class CoverSizeEnumConverter implements Converter<String, CoverSizeEnum> {
    @Override
    public CoverSizeEnum convert(String value) {
        return CoverSizeEnum.of(value);
    }
}
