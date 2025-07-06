package com.example.demo.service;

import com.example.demo.dto.bsn_logic_dto.ActivityDto;
import com.example.demo.entity.ActivityEmployeeEntity;
import com.example.demo.entity.ActivityEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.mapper.ActivityMapper;
import com.example.demo.repository.ActivityAssignmentLogRepository;
import com.example.demo.repository.ActivityEmployeeRepository;
import com.example.demo.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityAssignmentLogRepository activityAssignmentLogRepository;
    private final ActivityMapper activityMapper;
    private final ActivityServiceHelper helper;
    private final ActivityEmployeeRepository activityEmployeeRepository;

    @Transactional
    public Page<ActivityDto> findAllActivities(int page, int size, String username,
                                               LocalDate startDateFromRequest, LocalDate endDateFromRequest,
                                               String monthFromRequest, String sortDirection) {

        var sortedPageable = helper.getSortedPageable(page, size, sortDirection);
        var activityPage = helper.fetchActivityEntities(startDateFromRequest, endDateFromRequest, monthFromRequest, sortedPageable);
        Page<ActivityDto> dtoPage = activityPage.map(activityMapper::activityEntityToDto);

        List<ActivityDto> updatedDtos = helper.mapActivityEmployeeToDtos(dtoPage);

        UserEntity user = helper.getUserByEmployeeCode(username);
        helper.setWorkdayFlag(activityPage, updatedDtos, user.getId());
        helper.markAssignedForUser(updatedDtos, user.getId());
        helper.markHistoryFlag(updatedDtos);

        return new PageImpl<>(updatedDtos, sortedPageable, dtoPage.getTotalElements());
    }


    @Transactional
    public ActivityDto markActivityAsOwn(ActivityDto activityDto, String username) {
        ActivityEntity activityEntity = activityRepository.findById(activityDto.getActivityId())
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        UserEntity user = helper.getUserByEmployeeCode(username);

        helper.saveOldProcedureAssignment(activityEntity);
        activityEmployeeRepository.deleteByActivityActivityId(activityEntity.getActivityId());

        ActivityEmployeeEntity newAssignment = new ActivityEmployeeEntity();
        newAssignment.setActivity(activityEntity);
        newAssignment.setEmployee(user);
        newAssignment.setUserModified(true);
        activityEmployeeRepository.save(newAssignment);

        ActivityDto dto = activityMapper.activityEntityToDto(activityEntity);
        dto.setAssignedToLoggedUser(true);
        dto.setHasHistory(true);
        dto.setEmployeesAssigned(Set.of(user.getFullName()));
        dto.setEmployeeIdsAssigned(Set.of(user.getId()));

        helper.setWorkdayFlagForSingleActivity(dto, activityEntity, user.getId());

        return dto;
    }


    @Transactional
    public ActivityDto returnToOldAssignment(ActivityDto activityDto, String username) {
        ActivityEntity activityEntity = activityRepository.findById(activityDto.getActivityId())
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        UserEntity user = helper.getUserByEmployeeCode(username);

        var assignmentHistoryEntries = activityAssignmentLogRepository
                .findByActivity_ActivityIdOrderByAssignedAtDesc(activityDto.getActivityId());

        activityEmployeeRepository.deleteByActivityActivityId(activityEntity.getActivityId());

        ActivityDto dto = activityMapper.activityEntityToDto(activityEntity);

        if (assignmentHistoryEntries.isEmpty()) {
            return helper.removeAssignment(activityEntity, dto, user);
        }

        var uniqueLatestEmployees = helper.getUniqueLatestEmployees(assignmentHistoryEntries);

        var restoredAssignments = uniqueLatestEmployees.values().stream()
                .map(employee -> {
                    ActivityEmployeeEntity assignment = new ActivityEmployeeEntity();
                    assignment.setActivity(activityEntity);
                    assignment.setEmployee(employee);
                    assignment.setUserModified(false);
                    return assignment;
                })
                .toList();

        activityEmployeeRepository.saveAll(restoredAssignments);

        return helper.mapToDto(dto, restoredAssignments, activityEntity, user);
    }


}
