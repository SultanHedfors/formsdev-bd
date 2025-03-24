package com.example.demo.mapper;


import com.example.demo.dto.UserRegistrationDto;
import com.example.demo.dto.bsn_logic_dto.ActivityDto;
import com.example.demo.entity.ActivityEntity;
import com.example.demo.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ActivityMapper {




    @Mapping(target = "activityId", ignore = true)
    ActivityEntity userRegistrationDtoToEntity(ActivityDto activityDto);


    ActivityDto userRegistrationEntityToDto(ActivityEntity activityEntity);

}
