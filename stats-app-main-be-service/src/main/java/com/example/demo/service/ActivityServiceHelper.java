package com.example.demo.service;

import com.example.demo.dto.bsn_logic_dto.ActivityDto;
import com.example.demo.entity.ActivityAssignmentLogEntity;
import com.example.demo.entity.ActivityEmployeeEntity;
import com.example.demo.entity.ActivityEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class ActivityServiceHelper {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final ActivityAssignmentLogRepository activityAssignmentLogRepository;
    private final ScheduleRepository scheduleRepository;
    private final ActivityEmployeeRepository activityEmployeeRepository;

    Page<ActivityEntity> fetchActivityEntities(LocalDate startDate, LocalDate endDate,
                                               String month, Pageable pageable) {
        if (month != null) {
            YearMonth ym = YearMonth.parse(month);
            return activityRepository.findByActivityDateBetween(
                    ym.atDay(1).atStartOfDay(),
                    ym.atEndOfMonth().atTime(LocalTime.MAX),
                    pageable);
        }
        if (startDate != null && endDate != null) {
            return activityRepository.findByActivityDateBetween(
                    startDate.atStartOfDay(),
                    endDate.atTime(LocalTime.MAX),
                    pageable);
        }
        return activityRepository.findAll(pageable);
    }

    UserEntity getUserByEmployeeCode(String username) {
        return userRepository.findByEmployeeCode(username)
                .orElseThrow(() -> new RuntimeException("User not found for employee code: " + username));
    }

    Map<Integer, UserEntity> getUniqueLatestEmployees(List<ActivityAssignmentLogEntity> assignmentHistoryEntries) {
        return assignmentHistoryEntries.stream()
                .map(ActivityAssignmentLogEntity::getEmployee)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        UserEntity::getId,
                        emp -> emp,
                        (existing, duplicate) -> existing
                ));
    }

    Pageable getSortedPageable(int page, int size, String sortDirection) {
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ?
                Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, "activityDate", "activityTime"));
    }

    @Transactional
    List<ActivityDto> mapActivityEmployeeToDtos(Page<ActivityDto> dtoPage) {
        List<Integer> activityIds = dtoPage.getContent()
                .stream().map(ActivityDto::getActivityId).toList();
        var assignments = activityEmployeeRepository.findByActivityActivityIdIn(activityIds);

        Map<Integer, List<UserEntity>> activityEmployeeMap = assignments.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getActivity().getActivityId(),
                        Collectors.mapping(ActivityEmployeeEntity::getEmployee, Collectors.toList())
                ));
        return mapEmployeeDataToDtos(dtoPage, activityEmployeeMap);
    }

    ActivityDto mapToDto(ActivityDto dto, List<ActivityEmployeeEntity> restoredAssignments,
                         ActivityEntity activityEntity, UserEntity user) {
        dto.setHasHistory(true);
        dto.setEmployeesAssigned(restoredAssignments.stream()
                .map(a -> a.getEmployee().getFullName()).collect(Collectors.toSet()));
        dto.setEmployeeIdsAssigned(restoredAssignments.stream()
                .map(a -> a.getEmployee().getId()).collect(Collectors.toSet()));
        setWorkdayFlagForSingleActivity(dto, activityEntity, user.getId());
        return dto;
    }

    void markAssignedForUser(List<ActivityDto> dtoList, Integer userId) {
        dtoList.forEach(dto -> {
            Set<Integer> employeeIds = Optional.ofNullable(dto.getEmployeeIdsAssigned())
                    .orElse(Collections.emptySet());
            dto.setAssignedToLoggedUser(employeeIds.contains(userId));
        });
    }

    void markHistoryFlag(List<ActivityDto> dtoList) {
        List<Integer> activityIds = dtoList.stream().map(ActivityDto::getActivityId).toList();
        Set<Integer> historySet = new HashSet<>(activityAssignmentLogRepository
                .findExistingActivityIdsInLog(activityIds));
        dtoList.forEach(dto -> dto.setHasHistory(historySet.contains(dto.getActivityId())));
    }

    void setWorkdayFlag(Page<ActivityEntity> activityPage, List<ActivityDto> dtoList, Integer employeeId) {
        List<ActivityEntity> entities = activityPage.getContent();
        IntStream.range(0, entities.size())
                .filter(i -> entities.get(i).getActivityDate() != null)
                .forEach(i -> setWorkdayFlagForSingleActivity(dtoList.get(i), entities.get(i), employeeId));
    }

    void setWorkdayFlagForSingleActivity(ActivityDto dto, ActivityEntity entity, Integer employeeId) {
        if (entity.getActivityDate() == null) {
            dto.setProcedureScheduledOnEmployeesWorkingDay(false);
            return;
        }
        LocalDate date = entity.getActivityDate().toLocalDate();
        String yearMonthStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        int dayOfMonth = date.getDayOfMonth();
        boolean exists = scheduleRepository
                .existsByEmployee_IdAndYearMonthAndDayOfMonth(employeeId, yearMonthStr, dayOfMonth);
        dto.setProcedureScheduledOnEmployeesWorkingDay(exists);
    }

    ActivityDto removeAssignment(ActivityEntity activityEntity, ActivityDto dto, UserEntity user) {
        log.info("No assignment history found for activity {}. Current assignments removed.",
                activityEntity.getActivityId());
        dto.setHasHistory(false);
        dto.setEmployeesAssigned(Collections.emptySet());
        dto.setEmployeeIdsAssigned(Collections.emptySet());
        setWorkdayFlagForSingleActivity(dto, activityEntity, user.getId());
        return dto;
    }

    void saveOldProcedureAssignment(ActivityEntity activityEntity) {
        var existingAssignments = activityEmployeeRepository
                .findByActivityActivityId(activityEntity.getActivityId());
        if (existingAssignments.isEmpty()) {
            log.info("No existing assignments to log for activity {}", activityEntity.getActivityId());
            return;
        }
        List<UserEntity> employees = existingAssignments.stream()
                .map(ActivityEmployeeEntity::getEmployee)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (employees.isEmpty()) {
            log.warn("No valid employee entities found for logging in activity {}", activityEntity.getActivityId());
            return;
        }
        for (UserEntity employee : employees) {
            ActivityAssignmentLogEntity logEntity = ActivityAssignmentLogEntity.builder()
                    .activity(activityEntity)
                    .employee(employee)
                    .assignedAt(LocalDateTime.now())
                    .build();
            activityAssignmentLogRepository.save(logEntity);
            log.debug("Saved assignment log: ACTIVITY ID={} -> EMPLOYEE ID={}",
                    activityEntity.getActivityId(), employee.getId());
        }
        log.info("Finished saving {} assignment log entries for activity {}",
                employees.size(), activityEntity.getActivityId());
    }


    private List<ActivityDto> mapEmployeeDataToDtos(Page<ActivityDto> dtoPage, Map<Integer, List<UserEntity>> activityEmployeeMap) {
        List<ActivityDto> dtoList = dtoPage.getContent();
        dtoList.forEach(dto -> {
            List<UserEntity> employees = activityEmployeeMap.getOrDefault(dto.getActivityId(), Collections.emptyList());
            dto.setEmployeesAssigned(employees.stream().map(UserEntity::getFullName).collect(Collectors.toSet()));
            dto.setEmployeeIdsAssigned(employees.stream().map(UserEntity::getId).collect(Collectors.toSet()));
        });
        return dtoList;
    }
}
