package com.example.demo.mapper;

import com.example.demo.dto.EntryDto;
import com.example.demo.entity.EntryEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface EntryMapper {

    //    @Mapping(target = "id", ignore = true)
    EntryEntity entryDtoToEntity(EntryDto entryDto);

    EntryDto entryEntityToDto(EntryEntity entryEntity);

}
