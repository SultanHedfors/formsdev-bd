package com.example.demo.service;

import com.example.demo.dto.bsn_logic_dto.ActivityDto;
import com.example.demo.entity.*;
import com.example.demo.mapper.ActivityMapper;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityAssignmentLogRepository activityAssignmentLogRepository;
    private final ScheduleRepository scheduleRepository;
    private final ActivityMapper activityMapper;
    private final UserRepository userRepository;
    private final ActivityEmployeeRepository activityEmployeeRepository;

    @Transactional
    public Page<ActivityDto> findAll(int page, int size, String username, LocalDate startDate, LocalDate endDate, String sortDirection) {
        UserEntity user = getUserByEmployeeCode(username);
        Sort.Direction direction = sortDirection.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, "activityDate", "activityTime"));

        Page<ActivityEntity> activityPage = fetchActivityEntities(startDate, endDate, pageable);
        Page<ActivityDto> dtoPage = activityPage.map(activityMapper::activityEntityToDto);

        List<ActivityDto> updatedDtos = mapActivityEmployeeToDtos(dtoPage);

        setWorkdayFlag(activityPage, updatedDtos, user.getId());
        markAssignedForUser(updatedDtos, user.getId());
        markHistoryFlag(updatedDtos);

        return new PageImpl<>(updatedDtos, pageable, dtoPage.getTotalElements());
    }

    private UserEntity getUserByEmployeeCode(String username) {
        return userRepository.findByEmployeeCode(username)
                .orElseThrow(() -> new RuntimeException("User not found for employee code: " + username));
    }

    private Page<ActivityEntity> fetchActivityEntities(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            return activityRepository.findByActivityDateBetween(start, end, pageable);
        }
        return activityRepository.findAll(pageable);
    }

    private void markAssignedForUser(List<ActivityDto> dtoList, Integer userId) {
        dtoList.forEach(dto -> {
            List<Integer> employeeIds = Optional.ofNullable(dto.getEmployeeIdsAssigned()).orElse(Collections.emptyList());
            dto.setAssignedToLoggedUser(employeeIds.contains(userId));
        });
    }

    private void markHistoryFlag(List<ActivityDto> dtoList) {
        List<Integer> activityIds = dtoList.stream()
                .map(ActivityDto::getActivityId)
                .toList();

        Set<Integer> historySet = new HashSet<>(activityAssignmentLogRepository.findExistingActivityIdsInLog(activityIds));

        dtoList.forEach(dto -> dto.setHasHistory(historySet.contains(dto.getActivityId())));
    }

    @Transactional
    public List<ActivityDto> mapActivityEmployeeToDtos(Page<ActivityDto> dtoPage) {
        List<Integer> activityIds = dtoPage.getContent().stream()
                .map(ActivityDto::getActivityId)
                .toList();

        var assignments = activityEmployeeRepository.findByActivityActivityIdIn(activityIds);

        Map<Integer, List<UserEntity>> activityEmployeeMap = assignments.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getActivity().getActivityId(),
                        Collectors.mapping(ActivityEmployeeEntity::getEmployee, Collectors.toList())
                ));

        List<ActivityDto> dtoList = dtoPage.getContent();
        dtoList.forEach(dto -> {
            List<UserEntity> employees = activityEmployeeMap.getOrDefault(dto.getActivityId(), Collections.emptyList());

            dto.setEmployeesAssigned(employees.stream()
                    .map(UserEntity::getFullName)
                    .distinct()
                    .toList());

            dto.setEmployeeIdsAssigned(employees.stream()
                    .map(UserEntity::getId)
                    .distinct()
                    .toList());
        });

        return dtoList;
    }

    private void setWorkdayFlag(Page<ActivityEntity> activityPage, List<ActivityDto> dtoList, Integer employeeId) {
        List<ActivityEntity> entities = activityPage.getContent();
        DateTimeFormatter ymFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        IntStream.range(0, entities.size())
                .filter(i -> entities.get(i).getActivityDate() != null)
                .forEach(i -> {
                    ActivityEntity entity = entities.get(i);
                    ActivityDto dto = dtoList.get(i);
                    setWorkdayFlagForSingleActivity(dto, entity, employeeId);
                });
    }

    @Transactional
    public ActivityDto markActivityAsOwn(ActivityDto activityDto, String username) {
        ActivityEntity activityEntity = activityRepository.findById(activityDto.getActivityId())
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        UserEntity user = getUserByEmployeeCode(username);

        saveOldProcedureAssignment(activityEntity);
        activityEmployeeRepository.deleteByActivityActivityId(activityEntity.getActivityId());

        ActivityEmployeeEntity newAssignment = new ActivityEmployeeEntity();
        newAssignment.setActivity(activityEntity);
        newAssignment.setEmployee(user);
        newAssignment.setUserModified(true);
        activityEmployeeRepository.save(newAssignment);

        ActivityDto dto = activityMapper.activityEntityToDto(activityEntity);
        dto.setAssignedToLoggedUser(true);
        dto.setHasHistory(true);
        dto.setEmployeesAssigned(List.of(user.getFullName()));
        dto.setEmployeeIdsAssigned(List.of(user.getId()));

        // Dodajemy ustawienie workDay flagi!
        setWorkdayFlagForSingleActivity(dto, activityEntity, user.getId());

        return dto;
    }

    private void saveOldProcedureAssignment(ActivityEntity activityEntity) {
        List<ActivityEmployeeEntity> existingAssignments = activityEmployeeRepository.findByActivityActivityId(activityEntity.getActivityId());

        if (existingAssignments.isEmpty()) {
            log.info("No existing assignments to log for activity {}", activityEntity.getActivityId());
            return;
        }

        List<UserEntity> employees = existingAssignments.stream()
                .map(ActivityEmployeeEntity::getEmployee)
                .toList();

        ActivityAssignmentLogEntity logEntity = ActivityAssignmentLogEntity.builder()
                .activity(activityEntity)
                .employees(employees)
                .assignedAt(LocalDateTime.now())
                .build();

        activityAssignmentLogRepository.save(logEntity);
        log.info("Saved assignment log for activity {} with {} employees", activityEntity.getActivityId(), employees.size());
    }

    @Transactional
    public ActivityDto returnToOldAssignment(ActivityDto activityDto, String username) {
        ActivityEntity activityEntity = activityRepository.findById(activityDto.getActivityId())
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        UserEntity user = getUserByEmployeeCode(username);

        Optional<ActivityAssignmentLogEntity> optionalLogEntity = activityAssignmentLogRepository
                .findTopByActivity_ActivityIdOrderByAssignedAtDesc(activityDto.getActivityId());

        activityEmployeeRepository.deleteByActivityActivityId(activityEntity.getActivityId());

        ActivityDto dto = activityMapper.activityEntityToDto(activityEntity);

        if (optionalLogEntity.isEmpty()) {
            log.info("No assignment history found for activity {}. Current assignments removed.", activityEntity.getActivityId());
            dto.setHasHistory(false);
            dto.setEmployeesAssigned(Collections.emptyList());
            dto.setEmployeeIdsAssigned(Collections.emptyList());
            setWorkdayFlagForSingleActivity(dto,activityEntity,user.getId());
            return dto;
        }

        ActivityAssignmentLogEntity logEntity = optionalLogEntity.get();

        List<ActivityEmployeeEntity> restoredAssignments = logEntity.getEmployees().stream()
                .map(employee -> {
                    ActivityEmployeeEntity assignment = new ActivityEmployeeEntity();
                    assignment.setActivity(activityEntity);
                    assignment.setEmployee(employee);
                    assignment.setUserModified(false);
                    return assignment;
                })
                .toList();

        activityEmployeeRepository.saveAll(restoredAssignments);

        dto.setHasHistory(true);
        dto.setEmployeesAssigned(logEntity.getEmployees().stream()
                .map(UserEntity::getFullName)
                .toList());
        dto.setEmployeeIdsAssigned(logEntity.getEmployees().stream()
                .map(UserEntity::getId)
                .toList());

        // Używamy username (userId), tak jak w markActivityAsOwn
        setWorkdayFlagForSingleActivity(dto, activityEntity, user.getId());

        return dto;
    }

    private void setWorkdayFlagForSingleActivity(ActivityDto dto, ActivityEntity entity, Integer employeeId) {
        if (entity.getActivityDate() == null) {
            dto.setProcedureScheduledOnEmployeesWorkingDay(false);
            return;
        }

        LocalDate date = entity.getActivityDate().toLocalDateTime().toLocalDate();
        String yearMonthStr = date.format(DateTimeFormatter.ofPattern("yyyy-MM"));
        int dayOfMonth = date.getDayOfMonth();

        boolean exists = scheduleRepository.existsByEmployee_IdAndYearMonthAndDayOfMonth(employeeId, yearMonthStr, dayOfMonth);

        dto.setProcedureScheduledOnEmployeesWorkingDay(exists);
    }
}
