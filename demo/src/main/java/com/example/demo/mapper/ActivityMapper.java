package com.example.demo.mapper;


import com.example.demo.dto.bsn_logic_dto.ActivityDto;
import com.example.demo.entity.ActivityEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ActivityMapper {




    @Mapping(target = "activityId", ignore = true)
    ActivityEntity userRegistrationDtoToEntity(ActivityDto activityDto);


    @Mapping(source = "employee.id", target = "employeeId")
    @Mapping(source = "employee.employeeCode", target = "employeeCode")
    @Mapping(source = "employee.fullName", target = "employeeFullName")
    @Mapping(source="procedure.procedureName",target = "procedureName")
    @Mapping(source="procedure.procedureType",target = "procedureType")
    ActivityDto activityEntityToDto(ActivityEntity activityEntity);

}
