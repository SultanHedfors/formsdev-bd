package com.example.demo.service;

import com.example.demo.dto.bsn_logic_dto.ActivityDto;
import com.example.demo.entity.ActivityAssignmentLogEntity;
import com.example.demo.entity.ActivityEmployeeEntity;
import com.example.demo.entity.ActivityEntity;
import com.example.demo.entity.UserEntity;
import com.example.demo.mapper.ActivityMapper;
import com.example.demo.repository.ActivityAssignmentLogRepository;
import com.example.demo.repository.ActivityEmployeeRepository;
import com.example.demo.repository.ActivityRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ActivityServiceTest {

    @Mock ActivityRepository activityRepository;
    @Mock ActivityAssignmentLogRepository activityAssignmentLogRepository;
    @Mock ActivityMapper activityMapper;
    @Mock ActivityServiceHelper helper;
    @Mock ActivityEmployeeRepository activityEmployeeRepository;
    @InjectMocks ActivityService activityService;

    UserEntity user1, user2;
    ActivityEntity activity1;
    ActivityDto dto1;
    Pageable pageable;

    @BeforeEach
    @SuppressWarnings("resource")
    void setUp() {
        MockitoAnnotations.openMocks(this);
        user1 = UserEntity.builder().id(1).fullName("User1").build();
        user2 = UserEntity.builder().id(2).fullName("User2").build();
        activity1 = new ActivityEntity();
        activity1.setActivityId(101);
        dto1 = new ActivityDto();
        pageable = PageRequest.of(0, 10, Sort.Direction.ASC, "any");
    }

    @Test
    void findAllActivities_basicFlow_success() {
        // arrange
        var activityPage = new PageImpl<>(List.of(activity1), pageable, 1);
        var mappedDtos = List.of(dto1);
        when(helper.getSortedPageable(0, 10, "ASC")).thenReturn(pageable);
        when(helper.fetchActivityEntities(any(), any(), any(), eq(pageable))).thenReturn(activityPage);
        when(activityMapper.activityEntityToDto(activity1)).thenReturn(dto1);
        when(helper.mapActivityEmployeeToDtos(any())).thenReturn(mappedDtos);
        when(helper.getUserByEmployeeCode("user1")).thenReturn(user1);

        // act
        var result = activityService.findAllActivities(0, 10, "user1", LocalDate.now(), LocalDate.now(), "2024-07", "ASC");

        // assert
        assertThat(result.getContent()).isEqualTo(mappedDtos);
        verify(helper).setWorkdayFlag(activityPage, mappedDtos, user1.getId());
        verify(helper).markAssignedForUser(mappedDtos, user1.getId());
        verify(helper).markHistoryFlag(mappedDtos);
    }

    @Test
    void markActivityAsOwn_shouldAssignToUserAndReturnDto() {
        // arrange
        ActivityDto inputDto = new ActivityDto();
        inputDto.setActivityId(201);
        ActivityEntity activity = new ActivityEntity();
        activity.setActivityId(201);
        when(activityRepository.findById(201)).thenReturn(Optional.of(activity));
        when(helper.getUserByEmployeeCode("user2")).thenReturn(user2);
        when(activityMapper.activityEntityToDto(activity)).thenReturn(dto1);

        // act
        var result = activityService.markActivityAsOwn(inputDto, "user2");

        // assert
        verify(helper).saveOldProcedureAssignment(activity);
        verify(activityEmployeeRepository).deleteByActivityActivityId(201);
        verify(activityEmployeeRepository).save(any(ActivityEmployeeEntity.class));
        assertThat(result.isAssignedToLoggedUser()).isTrue();
        assertThat(result.isHasHistory()).isTrue();
        assertThat(result.getEmployeesAssigned()).contains("User2");
        assertThat(result.getEmployeeIdsAssigned()).contains(2);
        verify(helper).setWorkdayFlagForSingleActivity(result, activity, 2);
    }

    @Test
    void returnToOldAssignment_whenNoHistory_removesAssignment() {
        // arrange
        ActivityDto inputDto = new ActivityDto();
        inputDto.setActivityId(301);
        ActivityEntity activity = new ActivityEntity();
        activity.setActivityId(301);
        when(activityRepository.findById(301)).thenReturn(Optional.of(activity));
        when(helper.getUserByEmployeeCode("user1")).thenReturn(user1);
        when(activityAssignmentLogRepository.findByActivity_ActivityIdOrderByAssignedAtDesc(301)).thenReturn(List.of());
        when(activityMapper.activityEntityToDto(activity)).thenReturn(dto1);

        // act
        activityService.returnToOldAssignment(inputDto, "user1");

        // assert
        verify(activityEmployeeRepository).deleteByActivityActivityId(301);
        verify(helper).removeAssignment(activity, dto1, user1);
    }

    @Test
    void returnToOldAssignment_withHistory_restoresAssignments() {
        // arrange
        ActivityDto inputDto = new ActivityDto();
        inputDto.setActivityId(400);
        ActivityEntity activity = new ActivityEntity();
        activity.setActivityId(400);
        when(activityRepository.findById(400)).thenReturn(Optional.of(activity));
        when(helper.getUserByEmployeeCode("user2")).thenReturn(user2);

        UserEntity restoredUser = UserEntity.builder().id(2).fullName("User2").build();
        var assignmentHistory = List.of(mock(ActivityAssignmentLogEntity.class));
        when(activityAssignmentLogRepository.findByActivity_ActivityIdOrderByAssignedAtDesc(400)).thenReturn(assignmentHistory);
        when(activityMapper.activityEntityToDto(activity)).thenReturn(dto1);
        when(helper.getUniqueLatestEmployees(assignmentHistory)).thenReturn(Map.of(2, restoredUser));
        when(helper.mapToDto(any(), anyList(), any(), any())).thenReturn(dto1);

        // act
        var result = activityService.returnToOldAssignment(inputDto, "user2");

        // assert
        verify(activityEmployeeRepository).deleteByActivityActivityId(400);
        verify(activityEmployeeRepository).saveAll(anyList());
        verify(helper).mapToDto(eq(dto1), anyList(), eq(activity), eq(user2));
        assertThat(result).isSameAs(dto1);
    }
}

