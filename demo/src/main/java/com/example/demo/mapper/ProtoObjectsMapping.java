package com.example.demo.mapper;

import com.example.demo.entity.WorkSchedule;
import com.example.demo.grpc.Schedules;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProtoObjectsMapping {

    public List<Schedules.WorkSchedule> scheduleEntityToProtoObjMapper(List<WorkSchedule> workSchedules) {
        return workSchedules.stream().map(r ->
                        Schedules.WorkSchedule.newBuilder()
                                .setId(r.getId())
                                .setYearMonth(r.getYearMonth())
                                .setDayOfMonth(r.getDayOfMonth())
                                .setEmployeeName(r.getEmployee().getFullName())
                                .setSubstituteEmployeeName(r.getSubstituteEmployee().getFullName())
                                .setRoomSymbol(r.getRoomSymbol())
                                .setWorkMode(r.getWorkMode())
                                .setWorkStartTime(r.getWorkStartTime())
                                .setWorkEndTime(r.getWorkEndTime())
                                .setWorkDurationMinutes(r.getWorkDurationMinutes())
                                .build())
                .toList();
    }
}
