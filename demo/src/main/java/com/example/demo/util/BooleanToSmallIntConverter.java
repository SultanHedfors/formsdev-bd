package com.example.demo.util;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class BooleanToSmallIntConverter implements AttributeConverter<Boolean, Integer> {

    @Override
    public Integer convertToDatabaseColumn(Boolean value) {
        return (value != null && value) ? 1 : 0;
    }

    @Override
    public Boolean convertToEntityAttribute(Integer value) {
        return value != null && value == 1;
    }
}

