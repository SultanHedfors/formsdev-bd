package com.example.demo.mapper;

import com.example.demo.entity.WorkSchedule;
import grpc.Schedules;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProtoObjectsMapping {

    public List<Schedules.WorkSchedule> scheduleEntityToProtoObjMapper(List<WorkSchedule> workSchedules) {
        return workSchedules.stream().map(r -> {
            var subEmployee = r.getSubstituteEmployee();
            var roomSymbol = r.getRoomSymbol();
            var builder = Schedules.WorkSchedule.newBuilder();
            if(subEmployee!=null) {
                builder.setSubstituteEmployeeName(subEmployee.getFullName());
            }
            if(roomSymbol != null) {
                builder.setRoomSymbol(roomSymbol);
            }

            return builder
                    .setId(r.getId())
                    .setYearMonth(r.getYearMonth())
                    .setDayOfMonth(r.getDayOfMonth())
                    .setEmployeeName(r.getEmployee().getFullName())
                    .setWorkMode(r.getWorkMode())
                    .setWorkStartTime(r.getWorkStartTime())
                    .setWorkEndTime(r.getWorkEndTime())
                    .setWorkDurationMinutes(r.getWorkDurationMinutes())
                    .build();
        }).toList();
    }
}
