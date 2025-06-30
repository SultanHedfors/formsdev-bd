package com.example.demo.mapper;


import com.example.demo.dto.UserRegistrationDto;
import com.example.demo.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {
    @Mapping(target = "id", ignore = true)
    UserEntity userRegistrationDtoToEntity(UserRegistrationDto userRegistrationDto);

}
