package com.example.demo.mapper;

import com.example.demo.dto.EntryDto;
import com.example.demo.entity.EntryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.stereotype.Component;

@Mapper(componentModel = "spring")
public interface EntryMapper {

//    @Mapping(target = "id", ignore = true)
    public EntryEntity entryDtoToEntity(EntryDto entryDto);
    public EntryDto entryEntityToDto(EntryEntity entryEntity);

}
