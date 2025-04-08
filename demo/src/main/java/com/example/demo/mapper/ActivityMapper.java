package com.example.demo.mapper;

import com.example.demo.dto.bsn_logic_dto.ActivityDto;
import com.example.demo.entity.ActivityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ActivityMapper {

    // Mapowanie rejestracji użytkownika, ewentualnie inne metody...
    @Mapping(target = "activityId", ignore = true)
    ActivityEntity userRegistrationDtoToEntity(ActivityDto activityDto);

    // W tej metodzie ignorujemy mapowanie pól z employee,
    // aby w DTO nie były ustawiane employeeCode i employeeFullName automatycznie
    @Mapping(target = "employeeId", ignore = true)
    @Mapping(target = "employeeCode", ignore = true)
    @Mapping(target = "employeeFullName", ignore = true)
    @Mapping(source = "procedure.procedureName", target = "procedureName")
    @Mapping(source = "procedure.procedureType", target = "procedureType")
    ActivityDto activityEntityToDto(ActivityEntity activityEntity);
}
