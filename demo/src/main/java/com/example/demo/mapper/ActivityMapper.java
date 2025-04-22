package com.example.demo.mapper;

import com.example.demo.dto.bsn_logic_dto.ActivityDto;
import com.example.demo.entity.ActivityEntity;
import com.example.demo.entity.ProcedureEntity;
import org.hibernate.Hibernate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring")
public interface ActivityMapper {

    @Mapping(target = "activityId", ignore = true)
    ActivityEntity userRegistrationDtoToEntity(ActivityDto activityDto);

    @Mapping(target = "employeeId", ignore = true)
    @Mapping(target = "employeeCode", ignore = true)
    @Mapping(target = "employeeFullName", ignore = true)
    @Mapping(source = "procedure", target = "procedureName", qualifiedByName = "procedureNameResolver")
    @Mapping(source = "procedure", target = "procedureType", qualifiedByName = "procedureTypeResolver")
    @Mapping(source = "procedure", target = "workMode", qualifiedByName = "procedureWorkModeResolver") // 👈 nowy mapping
    ActivityDto activityEntityToDto(ActivityEntity activityEntity);

    @Named("procedureNameResolver")
    default String resolveProcedureName(ProcedureEntity procedureEntity) {
        if (procedureEntity == null || !Hibernate.isInitialized(procedureEntity)) {
            return null;
        }
        return procedureEntity.getProcedureName();
    }

    @Named("procedureTypeResolver")
    default String resolveProcedureType(ProcedureEntity procedureEntity) {
        if (procedureEntity == null || !Hibernate.isInitialized(procedureEntity)) {
            return null;
        }
        return procedureEntity.getProcedureType();
    }

    @Named("procedureWorkModeResolver") // 👈 nowa metoda
    default String resolveProcedureWorkMode(ProcedureEntity procedureEntity) {
        if (procedureEntity == null || !Hibernate.isInitialized(procedureEntity)) {
            return null;
        }
        return procedureEntity.getWorkMode();
    }
}
