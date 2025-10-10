package com.example.book_talker_backend.book.entity.dto;

import com.example.book_talker_backend.book.exception.UnknownEnumValueException;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum CoverSizeEnum {
    BIG("Big", "너비 200px"),
    MID_BIG("MidBig", "너비 150px"),
    MID("Mid", "너비 85px"),
    SMALL("Small", "너비 75px"),
    MINI("Mini", "너비 65px"),
    NONE("None", "없음");

    private final String value;
    private final String description;

    CoverSizeEnum(String value, String description) {
        this.value = value;
        this.description = description;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static CoverSizeEnum of(String value) {
        if (null == value) {
            return null;
        }

        for (CoverSizeEnum item : CoverSizeEnum.values()) {
            if (value.equals(item.getValue())) {
                return item;
            }
        }

        throw new UnknownEnumValueException("CoverSizeEnum: unknown value: " + value);
    }
}
