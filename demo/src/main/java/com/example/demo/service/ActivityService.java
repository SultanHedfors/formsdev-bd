package com.example.demo.service;

import com.example.demo.dto.bsn_logic_dto.ActivityDto;
import com.example.demo.dto.bsn_logic_dto.ProcedureDto;
import com.example.demo.entity.ActivityAssignmentLogEntity;
import com.example.demo.entity.ActivityEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.exception.ActivityAssignmentLogNotFoundException;
import com.example.demo.mapper.ActivityMapper;
import com.example.demo.repository.ActivityAssignmentLogRepository;
import com.example.demo.repository.ActivityRepository;
import com.example.demo.repository.ScheduleRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityAssignmentLogRepository activityAssignmentLogRepository;
    private ScheduleRepository workScheduleRepository;
    private final ActivityMapper activityMapper;
    private final UserRepository userRepository;

    public ActivityService(ActivityRepository activityRepository, ActivityAssignmentLogRepository activityAssignmentLogRepository, ScheduleRepository workScheduleRepository, ActivityMapper activityMapper, UserRepository userRepository) {
        this.activityRepository = activityRepository;
        this.activityAssignmentLogRepository = activityAssignmentLogRepository;
        this.workScheduleRepository = workScheduleRepository;
        this.activityMapper = activityMapper;
        this.userRepository = userRepository;
    }

    public Page<ActivityDto> findAll(int page, int size, String username, LocalDate startDate, LocalDate endDate) {
        final UserEntity user = userRepository.findByEmployeeCode(username)
                .orElseThrow(RuntimeException::new);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "activityDate", "activityTime"));

        Page<ActivityEntity> activitiesEntityPage;

        if (startDate != null && endDate != null) {
            LocalDateTime start = startDate.atStartOfDay();
            LocalDateTime end = endDate.atTime(LocalTime.MAX);
            activitiesEntityPage = activityRepository.findByActivityDateBetween(start, end, pageable);
        } else {
            activitiesEntityPage = activityRepository.findAll(pageable);
        }

        Page<ActivityDto> activitiesDtoPage = activitiesEntityPage.map(activityMapper::activityEntityToDto);
        setWorkdayFlag(activitiesEntityPage, activitiesDtoPage, user.getId());

        activitiesDtoPage.forEach(a -> {
            if (user.getId().equals(a.getEmployeeId())) {
                a.setAssignedToLoggedUser(true);
            }
        });

        List<Integer> activityIdsOnPage = activitiesDtoPage.stream()
                .map(ActivityDto::getActivityId)
                .toList();
        Set<Integer> historySet = new HashSet<>(activityAssignmentLogRepository.findExistingActivityIdsInLog(activityIdsOnPage));
        activitiesDtoPage.forEach(a -> a.setHasHistory(historySet.contains(a.getActivityId())));

        return activitiesDtoPage;
    }


    private void setWorkdayFlag(Page<ActivityEntity> activitiesEntityPage, Page<ActivityDto> activitiesDtoPage, Integer employeeId) {
        List<ActivityEntity> entities = activitiesEntityPage.getContent();
        List<ActivityDto> dtos = activitiesDtoPage.getContent();

        IntStream.range(0, entities.size())
                .filter(i -> entities.get(i).getActivityDate() != null && entities.get(i).getEmployee() != null)
                .forEach(i -> {
                    ActivityEntity entity = entities.get(i);
                    ActivityDto dto = dtos.get(i);

                    LocalDate date = entity.getActivityDate().toLocalDateTime().toLocalDate();
                    String yearMonth = String.format("%d-%02d", date.getYear(), date.getMonthValue());
                    int dayOfMonth = date.getDayOfMonth();

                    boolean exists = workScheduleRepository.existsByEmployee_IdAndYearMonthAndDayOfMonth(
                            employeeId, yearMonth, dayOfMonth
                    );

                    dto.setProcedureScheduledOnEmployeesWorkingDay(exists);
                });
    }

    public ProcedureDto findById(Long id) {
        // TODO: Implement service logic to retrieve a procedure by id
        return null;
    }

    public ProcedureDto save(ProcedureDto procedureDto) {
        // TODO: Implement service logic to save a new procedure
        return procedureDto;
    }

    public ActivityDto markActivityAsOwn(ActivityDto activityDto, String username) {
        final ActivityEntity activityEntity = activityRepository.findById(activityDto.getActivityId())
                .orElseThrow(RuntimeException::new);
        final UserEntity user = userRepository.findByEmployeeCode(username).orElseThrow(RuntimeException::new);
        saveOldProcedureAssignment(activityEntity);
        activityEntity.setEmployee(user);
        activityRepository.save(activityEntity);
        ActivityDto returnDto = activityMapper.activityEntityToDto(activityEntity);
        returnDto.setAssignedToLoggedUser(true);
        returnDto.setHasHistory(true);
        return returnDto;
    }

    private void saveOldProcedureAssignment(final ActivityEntity activityEntity){
        ActivityAssignmentLogEntity activityAssignmentLogEntity= ActivityAssignmentLogEntity.builder()
                .activity(activityEntity)
                .employee(activityEntity.getEmployee())
                .assignedAt(LocalDateTime.now()).build();
        activityAssignmentLogRepository.save(activityAssignmentLogEntity);
    }
    public ActivityDto returnToOldAssignment(ActivityDto activityDto) {
        final ActivityEntity activityEntity = activityRepository.findById(activityDto.getActivityId())
                .orElseThrow(RuntimeException::new);
        ActivityAssignmentLogEntity activityAssignmentLogEntity = activityAssignmentLogRepository
                .findTopByActivity_ActivityIdOrderByAssignedAtDesc(activityDto.getActivityId())
                .orElseThrow(()->new ActivityAssignmentLogNotFoundException(activityDto.getActivityId()));

        final UserEntity user = userRepository.findById(activityAssignmentLogEntity.getEmployee().getId()).orElseThrow(RuntimeException::new);

        activityEntity.setEmployee(user);
        activityRepository.save(activityEntity);
        saveOldProcedureAssignment(activityEntity);

        return activityMapper.activityEntityToDto(activityEntity);
    }
}
