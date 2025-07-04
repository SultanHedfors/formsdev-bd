package com.example.demo.service;

import com.example.demo.dto.bsn_logic_dto.ActivityDto;
import com.example.demo.entity.*;
import com.example.demo.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.data.domain.*;

import java.time.*;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class ActivityServiceHelperTest {

    @Mock
    UserRepository userRepository;
    @Mock
    ActivityRepository activityRepository;
    @Mock
    ActivityAssignmentLogRepository activityAssignmentLogRepository;
    @Mock
    ScheduleRepository scheduleRepository;
    @Mock
    ActivityEmployeeRepository activityEmployeeRepository;

    @InjectMocks
    ActivityServiceHelper helper;

    UserEntity user1, user2, user3;
    ActivityEntity activity1;
    Pageable pageable;
    Page<ActivityEntity> activityPage;
    ActivityAssignmentLogEntity log1, log2;

    @BeforeEach
    @SuppressWarnings("resource")
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user1 = UserEntity.builder().id(1).employeeCode("user1").fullName("User One").build();
        user2 = UserEntity.builder().id(2).employeeCode("user2").fullName("User Two").build();
        user3 = UserEntity.builder().id(3).employeeCode("user3").fullName("User Three").build();
        activity1 = ActivityEntity.builder().activityId(100).build();
        pageable = PageRequest.of(0, 10);
        activityPage = new PageImpl<>(List.of(activity1));
        log1 = ActivityAssignmentLogEntity.builder().employee(user1).build();
        log2 = ActivityAssignmentLogEntity.builder().employee(user2).build();
    }

    @Test
    void fetchActivityEntities_byMonth_returnsFromRepo() {
        // arrange
        String ym = "2024-07";
        when(activityRepository.findByActivityDateBetween(any(), any(), eq(pageable))).thenReturn(activityPage);

        // act
        Page<ActivityEntity> result = helper.fetchActivityEntities(null, null, ym, pageable);

        // assert
        assertThat(result).isEqualTo(activityPage);
    }

    @Test
    void fetchActivityEntities_byDate_returnsFromRepo() {
        // arrange
        LocalDate from = LocalDate.of(2024, 7, 1), to = LocalDate.of(2024, 7, 10);
        when(activityRepository.findByActivityDateBetween(any(), any(), eq(pageable))).thenReturn(activityPage);

        // act
        Page<ActivityEntity> result = helper.fetchActivityEntities(from, to, null, pageable);

        // assert
        assertThat(result).isEqualTo(activityPage);
    }

    @Test
    void fetchActivityEntities_all_returnsFromRepo() {
        // arrange
        when(activityRepository.findAll(pageable)).thenReturn(activityPage);

        // act
        Page<ActivityEntity> result = helper.fetchActivityEntities(null, null, null, pageable);

        // assert
        assertThat(result).isEqualTo(activityPage);
    }

    @Test
    void getUserByEmployeeCode_found_returnsUser() {
        // arrange
        when(userRepository.findByEmployeeCode("user1")).thenReturn(Optional.of(user1));

        // act
        UserEntity result = helper.getUserByEmployeeCode("user1");

        // assert
        assertThat(result).isEqualTo(user1);
    }

    @Test
    void getUserByEmployeeCode_notFound_throws() {
        // arrange
        when(userRepository.findByEmployeeCode("missingUser")).thenReturn(Optional.empty());

        // act & assert
        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> helper.getUserByEmployeeCode("missingUser"));
    }

    @Test
    void getUniqueLatestEmployees_mergesById() {
        // arrange
        List<ActivityAssignmentLogEntity> logs = List.of(log1, log2, log1);

        // act
        Map<Integer, UserEntity> result = helper.getUniqueLatestEmployees(logs);

        // assert
        assertThat(result.keySet()).containsExactlyInAnyOrder(1, 2);
        assertThat(result.get(1)).isSameAs(user1);
        assertThat(result.get(2)).isSameAs(user2);
    }

    @Test
    void getSortedPageable_returnsAscOrDesc() {
        // act
        Pageable asc = helper.getSortedPageable(0, 10, "asc");
        Pageable desc = helper.getSortedPageable(1, 20, "desc");

        // assert
        assertThat(Objects.requireNonNull(asc.getSort().getOrderFor("activityDate")).getDirection()).isEqualTo(Sort.Direction.ASC);
        assertThat(Objects.requireNonNull(desc.getSort().getOrderFor("activityDate")).getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void mapActivityEmployeeToDtos_setsAssignedFields() {
        // arrange
        ActivityDto dto = newDto(200);
        Page<ActivityDto> dtoPage = new PageImpl<>(List.of(dto));
        ActivityEmployeeEntity assignment = newAssignment(200, user2);
        when(activityEmployeeRepository.findByActivityActivityIdIn(List.of(200)))
                .thenReturn(List.of(assignment));

        // act
        List<ActivityDto> result = helper.mapActivityEmployeeToDtos(dtoPage);

        // assert
        assertThat(result.get(0).getEmployeeIdsAssigned()).contains(2);
        assertThat(result.get(0).getEmployeesAssigned()).contains("User Two");
    }

    @Test
    void mapToDto_setsHistoryAndEmployeesAndCallsWorkday() {
        // arrange
        ActivityDto dto = newDto(123);
        ActivityEntity activity = ActivityEntity.builder().activityId(123).build();
        List<ActivityEmployeeEntity> restored = List.of(newAssignment(123, user3));
        ActivityServiceHelper spy = Mockito.spy(helper);
        doNothing().when(spy).setWorkdayFlagForSingleActivity(any(), any(), any());

        // act
        ActivityDto result = spy.mapToDto(dto, restored, activity, user3);

        // assert
        assertThat(result.isHasHistory()).isTrue();
        assertThat(result.getEmployeeIdsAssigned()).contains(3);
        assertThat(result.getEmployeesAssigned()).contains("User Three");
        verify(spy).setWorkdayFlagForSingleActivity(dto, activity, 3);
    }

    @Test
    void markAssignedForUser_setsFlag() {
        // arrange
        ActivityDto dto = newDto(201);
        dto.setEmployeeIdsAssigned(Set.of(2, 3));
        List<ActivityDto> list = List.of(dto);

        // act & assert
        helper.markAssignedForUser(list, 2);
        assertThat(dto.isAssignedToLoggedUser()).isTrue();

        helper.markAssignedForUser(list, 7);
        assertThat(dto.isAssignedToLoggedUser()).isFalse();
    }

    @Test
    void markHistoryFlag_setsHasHistory() {
        // arrange
        ActivityDto dto = newDto(88);
        List<ActivityDto> dtos = List.of(dto);
        when(activityAssignmentLogRepository.findExistingActivityIdsInLog(List.of(88)))
                .thenReturn(List.of(88));

        // act
        helper.markHistoryFlag(dtos);

        // assert
        assertThat(dto.isHasHistory()).isTrue();
    }

    @Test
    void setWorkdayFlagForSingleActivity_setsFlagTrueOrFalse() {
        // arrange
        ActivityDto dto = newDto(202);
        ActivityEntity entity = ActivityEntity.builder().activityDate(LocalDateTime.of(2024, 7, 10, 10, 0)).build();
        when(scheduleRepository.existsByEmployee_IdAndYearMonthAndDayOfMonth(eq(2), any(), anyInt())).thenReturn(true);

        // act
        helper.setWorkdayFlagForSingleActivity(dto, entity, 2);

        // assert
        assertThat(dto.isProcedureScheduledOnEmployeesWorkingDay()).isTrue();

        // arrange 2
        when(scheduleRepository.existsByEmployee_IdAndYearMonthAndDayOfMonth(eq(2), any(), anyInt())).thenReturn(false);

        // act
        helper.setWorkdayFlagForSingleActivity(dto, entity, 2);

        // assert
        assertThat(dto.isProcedureScheduledOnEmployeesWorkingDay()).isFalse();
    }

    @Test
    void removeAssignment_removesAllAssignments() {
        // arrange
        ActivityDto dto = newDto(300);
        ActivityEntity entity = ActivityEntity.builder().activityId(300).build();
        ActivityServiceHelper spy = Mockito.spy(helper);
        doNothing().when(spy).setWorkdayFlagForSingleActivity(any(), any(), any());

        // act
        ActivityDto result = spy.removeAssignment(entity, dto, user1);

        // assert
        assertThat(result.isHasHistory()).isFalse();
        assertThat(result.getEmployeeIdsAssigned()).isEmpty();
        assertThat(result.getEmployeesAssigned()).isEmpty();
        verify(spy).setWorkdayFlagForSingleActivity(dto, entity, 1);
    }

    @Test
    void saveOldProcedureAssignment_logsForEachEmployee() {
        // arrange
        ActivityEntity entity = ActivityEntity.builder().activityId(42).build();
        ActivityEmployeeEntity a1 = newAssignment(42, user1);
        ActivityEmployeeEntity a2 = newAssignment(42, user2);
        when(activityEmployeeRepository.findByActivityActivityId(42)).thenReturn(List.of(a1, a2));

        // act
        helper.saveOldProcedureAssignment(entity);

        // assert
        verify(activityAssignmentLogRepository, times(2)).save(any(ActivityAssignmentLogEntity.class));
    }

    @Test
    void saveOldProcedureAssignment_emptyAssignments_logsInfo() {
        // arrange
        ActivityEntity entity = ActivityEntity.builder().activityId(77).build();
        when(activityEmployeeRepository.findByActivityActivityId(77)).thenReturn(Collections.emptyList());

        // act & assert
        assertDoesNotThrow(() -> helper.saveOldProcedureAssignment(entity));

    }

    // Helper Methods
    private ActivityDto newDto(int id) {
        ActivityDto dto = new ActivityDto();
        dto.setActivityId(id);
        return dto;
    }

    private ActivityEmployeeEntity newAssignment(int activityId, UserEntity user) {
        return ActivityEmployeeEntity.builder()
                .activity(ActivityEntity.builder().activityId(activityId).build())
                .employee(user)
                .build();
    }
}

