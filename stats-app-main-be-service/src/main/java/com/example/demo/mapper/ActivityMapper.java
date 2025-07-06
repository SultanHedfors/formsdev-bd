package com.example.demo.mapper;

import com.example.demo.dto.bsn_logic_dto.ActivityDto;
import com.example.demo.entity.ActivityEntity;
import com.example.demo.entity.ProcedureEntity;
import org.hibernate.Hibernate;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Mapper(componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface ActivityMapper {

    @Mapping(target = "employeeId", ignore = true)
    @Mapping(target = "employeeCode", ignore = true)
    @Mapping(target = "employeeFullName", ignore = true)
    @Mapping(source = "procedure", target = "procedureName", qualifiedByName = "procedureNameResolver")
    @Mapping(source = "procedure", target = "procedureType", qualifiedByName = "procedureTypeResolver")
    @Mapping(source = "procedure", target = "workMode", qualifiedByName = "procedureWorkModeResolver")
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

    @Named("procedureWorkModeResolver")
    default String resolveProcedureWorkMode(ProcedureEntity procedureEntity) {
        if (procedureEntity == null || !Hibernate.isInitialized(procedureEntity)) {
            return null;
        }
        return procedureEntity.getWorkMode();
    }

    //For Dto-Entity datetime format compatibility
    default Timestamp map(LocalDateTime value) {
        return value == null ? null : Timestamp.valueOf(value);
    }

    default LocalDateTime map(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
