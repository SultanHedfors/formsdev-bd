package com.example.demo.service;

import com.example.demo.dto.bsn_logic_dto.ActivityDto;
import com.example.demo.entity.ActivityAssignmentLogEntity;
import com.example.demo.entity.ActivityEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.entity.WorkSchedule;
import com.example.demo.exception.ActivityAssignmentLogNotFoundException;
import com.example.demo.mapper.ActivityMapper;
import com.example.demo.repository.ActivityAssignmentLogRepository;
import com.example.demo.repository.ActivityRepository;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.repository.UserRepository;
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

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityAssignmentLogRepository activityAssignmentLogRepository;
    private final ScheduleRepository scheduleRepository;
    private final ActivityMapper activityMapper;
    private final UserRepository userRepository;

    public ActivityService(ActivityRepository activityRepository,
                           ActivityAssignmentLogRepository activityAssignmentLogRepository,
                           ScheduleRepository scheduleRepository,
                           ActivityMapper activityMapper,
                           UserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.activityAssignmentLogRepository = activityAssignmentLogRepository;
        this.scheduleRepository = scheduleRepository;
        this.activityMapper = activityMapper;
        this.userRepository = userRepository;
    }

    @Transactional
    public Page<ActivityDto> findAll(int page, int size, String username, LocalDate startDate, LocalDate endDate) {
        UserEntity user = getUserByEmployeeCode(username);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "activityDate", "activityTime"));

        Page<ActivityEntity> activityPage = fetchActivityEntities(startDate, endDate, pageable);
        Page<ActivityDto> dtoPage = activityPage.map(activityMapper::activityEntityToDto);

        setWorkdayFlag(activityPage, dtoPage, user.getId());
        markAssignedForUser(dtoPage, user);
        markHistoryFlag(dtoPage);

        List<ActivityDto> updatedDtos = mapWorkScheduleToDtos(dtoPage);
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

    private void markAssignedForUser(Page<ActivityDto> dtoPage, UserEntity user) {
        dtoPage.forEach(dto -> {
            if (user.getId().equals(dto.getEmployeeId())) {
                dto.setAssignedToLoggedUser(true);
            }
        });
    }

    private void markHistoryFlag(Page<ActivityDto> dtoPage) {
        List<Integer> activityIds = dtoPage.stream()
                .map(ActivityDto::getActivityId)
                .collect(Collectors.toList());

        Set<Integer> historySet = new HashSet<>(activityAssignmentLogRepository.findExistingActivityIdsInLog(activityIds));

        dtoPage.forEach(dto -> dto.setHasHistory(historySet.contains(dto.getActivityId())));
    }

    @Transactional
    public List<ActivityDto> mapWorkScheduleToDtos(Page<ActivityDto> dtoPage) {
        Set<String> dateSet = dtoPage.stream()
                .map(dto -> dto.getActivityDate().toLocalDateTime().toLocalDate().toString())
                .collect(Collectors.toSet());

        List<WorkSchedule> schedules = scheduleRepository.findByDateIn(dateSet);

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        DateTimeFormatter ymFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        // Usunięto mapowanie activityEntityMap — niepotrzebne przy odczycie
        return dtoPage.getContent().stream()
                .map(dto -> applyWorkScheduleMapping(dto, schedules))
                .collect(Collectors.toList());
    }

    private ActivityDto applyWorkScheduleMapping(ActivityDto dto,
                                                 List<WorkSchedule> schedules) {

        Optional<WorkSchedule> matchingSchedule = schedules.stream()
                .filter(ws -> ws.getActivity() != null && ws.getActivity().getActivityId().equals(dto.getActivityId()))
                .findFirst();

        matchingSchedule.ifPresent(ws -> {
            if (ws.getRoomSymbol() != null && !ws.getRoomSymbol().isEmpty()) {
                dto.setRoomCode(ws.getRoomSymbol());
                dto.setEmployeeFullName("");
                if (ws.getEmployee() != null) {
                    dto.setEmployeeCode(ws.getEmployee().getEmployeeCode());
                    dto.setEmployeesAssigned(List.of(ws.getEmployee().getFullName()));
                } else {
                    dto.setEmployeeCode("Unknown");
                    dto.setEmployeesAssigned(List.of("Unknown"));
                }
            } else {
                dto.setEmployeeFullName("");
                dto.setEmployeeCode(ws.getWorkMode());
                List<String> assignedEmployeeNames = schedules.stream()
                        .filter(s -> s.getActivity() != null
                                && s.getActivity().getActivityId().equals(dto.getActivityId())
                                && s.getWorkMode().equals(ws.getWorkMode()))
                        .map(s -> s.getEmployee() != null ? s.getEmployee().getFullName() : "Unknown")
                        .distinct()
                        .collect(Collectors.toList());
                dto.setEmployeesAssigned(assignedEmployeeNames);
            }
        });

        return dto;
    }


    private void setWorkdayFlag(Page<ActivityEntity> activityPage, Page<ActivityDto> dtoPage, Integer employeeId) {
        List<ActivityEntity> entities = activityPage.getContent();
        List<ActivityDto> dtos = dtoPage.getContent();
        DateTimeFormatter ymFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

        IntStream.range(0, entities.size())
                .filter(i -> entities.get(i).getActivityDate() != null && entities.get(i).getEmployee() != null)
                .forEach(i -> {
                    ActivityEntity entity = entities.get(i);
                    ActivityDto dto = dtos.get(i);
                    LocalDate date = entity.getActivityDate().toLocalDateTime().toLocalDate();
                    String yearMonthStr = date.format(ymFormatter);
                    int dayOfMonth = date.getDayOfMonth();
                    boolean exists = scheduleRepository.existsByEmployee_IdAndYearMonthAndDayOfMonth(
                            employeeId, yearMonthStr, dayOfMonth
                    );
                    dto.setProcedureScheduledOnEmployeesWorkingDay(exists);
                });
    }

    @Transactional
    public ActivityDto markActivityAsOwn(ActivityDto activityDto, String username) {
        ActivityEntity activityEntity = activityRepository.findById(activityDto.getActivityId())
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        UserEntity user = userRepository.findByEmployeeCode(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        saveOldProcedureAssignment(activityEntity);
        activityEntity.setEmployee(user);
        activityRepository.save(activityEntity);

        ActivityDto dto = activityMapper.activityEntityToDto(activityEntity);
        dto.setAssignedToLoggedUser(true);
        dto.setHasHistory(true);
        return dto;
    }

    private void saveOldProcedureAssignment(ActivityEntity activityEntity) {
        ActivityAssignmentLogEntity logEntity = ActivityAssignmentLogEntity.builder()
                .activity(activityEntity)
                .assignedAt(LocalDateTime.now())
                .build();
        activityAssignmentLogRepository.save(logEntity);
    }

    @Transactional
    public ActivityDto returnToOldAssignment(ActivityDto activityDto) {
        ActivityEntity activityEntity = activityRepository.findById(activityDto.getActivityId())
                .orElseThrow(() -> new RuntimeException("Activity not found"));

        ActivityAssignmentLogEntity logEntity = activityAssignmentLogRepository
                .findTopByActivity_ActivityIdOrderByAssignedAtDesc(activityDto.getActivityId())
                .orElseThrow(() -> new ActivityAssignmentLogNotFoundException(activityDto.getActivityId()));

        activityRepository.save(activityEntity);
        saveOldProcedureAssignment(activityEntity);
        return activityMapper.activityEntityToDto(activityEntity);
    }
}
