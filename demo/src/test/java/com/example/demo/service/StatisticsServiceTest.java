package com.example.demo.service;

import com.example.demo.dto.bsn_logic_dto.StatisticsDto;
import com.example.demo.dto.stats_dtos.DailyStatsDto;
import com.example.demo.entity.UserEntity;
import com.example.demo.entity.stats.EmployeeDailyStatsEntity;
import com.example.demo.mapper.EmployeeStatisticsMapper;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.stats.DailyEmployeeStatisticRepository;
import com.example.demo.repository.stats.MonthlyEmployeeStatisticRepository;
import com.example.demo.repository.stats.WeeklyEmployeeStatisticRepository;
import com.example.demo.repository.stats.YearlyEmployeeStatisticRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StatisticsServiceTest {

    private UserRepository userRepository;
    private DailyEmployeeStatisticRepository dailyRepo;
    private EmployeeStatisticsMapper mapper;

    private StatisticsService service;

    @BeforeEach
    void setUp() {
        var weeklyRepo = mock(WeeklyEmployeeStatisticRepository.class);
        var monthlyRepo = mock(MonthlyEmployeeStatisticRepository.class);
        var yearlyRepo = mock(YearlyEmployeeStatisticRepository.class);
        userRepository = mock(UserRepository.class);
        dailyRepo = mock(DailyEmployeeStatisticRepository.class);
        mapper = mock(EmployeeStatisticsMapper.class);

        service = new StatisticsService(
                userRepository, dailyRepo, weeklyRepo, monthlyRepo, yearlyRepo, mapper
        );
    }

    @Test
    void getStatistics_returnsStatsForUsersWithAnyStats() {
        // Arrange
        var user1 = new UserEntity();
        user1.setId(1);
        user1.setFullName("Alice");
        user1.setEmployeeCode("ALICE");

        var user2 = new UserEntity();
        user2.setId(2);
        user2.setFullName("Bob");
        user2.setEmployeeCode("BOB");

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        try (MockedStatic<com.example.demo.util.AuthUtil> mock = mockStatic(com.example.demo.util.AuthUtil.class)) {
            mock.when(com.example.demo.util.AuthUtil::userFromSecurityContext).thenReturn("ALICE");

            var entity = mock(EmployeeDailyStatsEntity.class);
            when(dailyRepo.findTop100ByEmployeeIdOrderByStartDayDesc(1)).thenReturn(List.of(entity));
            when(mapper.toDailyDtoList(List.of(entity))).thenReturn(List.of(mock(DailyStatsDto.class)));

            when(dailyRepo.findTop100ByEmployeeIdOrderByStartDayDesc(2)).thenReturn(List.of());

            when(mapper.toDailyDtoList(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toWeeklyDtoList(anyList())).thenReturn(List.of());
            when(mapper.toMonthlyDtoList(anyList())).thenReturn(List.of());
            when(mapper.toYearlyDtoList(anyList())).thenReturn(List.of());

            // Act
            StatisticsDto result = service.getStatistics();

            // Assert
            assertThat(result).isNotNull();
            assertThat(result.getEmployees()).hasSize(1);

            var employeeStat = result.getEmployees().get(0);
            assertThat(employeeStat.getEmployeeName()).isEqualTo("Alice");
            assertThat(employeeStat.isCurrentUser()).isTrue();
            assertThat(employeeStat.getDailyStats()).hasSize(1);
        }
    }

    @Test
    void getStatistics_sortsCurrentUserOnTop() {
        // Arrange
        var user1 = new UserEntity();
        user1.setId(1);
        user1.setFullName("Alice");
        user1.setEmployeeCode("ALICE");

        var user2 = new UserEntity();
        user2.setId(2);
        user2.setFullName("Bob");
        user2.setEmployeeCode("BOB");

        when(userRepository.findAll()).thenReturn(List.of(user1, user2));

        try (MockedStatic<com.example.demo.util.AuthUtil> mock = mockStatic(com.example.demo.util.AuthUtil.class)) {
            mock.when(com.example.demo.util.AuthUtil::userFromSecurityContext).thenReturn("BOB");

            var dailyEntity1 = mock(EmployeeDailyStatsEntity.class);
            var dailyEntity2 = mock(EmployeeDailyStatsEntity.class);

            when(dailyRepo.findTop100ByEmployeeIdOrderByStartDayDesc(1)).thenReturn(List.of(dailyEntity1));
            when(dailyRepo.findTop100ByEmployeeIdOrderByStartDayDesc(2)).thenReturn(List.of(dailyEntity2));

            when(mapper.toDailyDtoList(List.of(dailyEntity1))).thenReturn(List.of(mock(DailyStatsDto.class)));
            when(mapper.toDailyDtoList(List.of(dailyEntity2))).thenReturn(List.of(mock(DailyStatsDto.class)));

            when(mapper.toDailyDtoList(anyList())).thenAnswer(inv -> inv.getArgument(0));
            when(mapper.toWeeklyDtoList(anyList())).thenReturn(List.of());
            when(mapper.toMonthlyDtoList(anyList())).thenReturn(List.of());
            when(mapper.toYearlyDtoList(anyList())).thenReturn(List.of());

            // Act
            StatisticsDto result = service.getStatistics();

            // Assert
            assertThat(result.getEmployees()).hasSize(2);
            assertThat(result.getEmployees().get(0).getEmployeeName()).isEqualTo("Bob");
            assertThat(result.getEmployees().get(0).isCurrentUser()).isTrue();
        }
    }

    @Test
    void getStatistics_returnsEmptyIfNoStats() {
        // Arrange
        var user1 = new UserEntity();
        user1.setId(1);
        user1.setFullName("Alice");
        user1.setEmployeeCode("ALICE");
        when(userRepository.findAll()).thenReturn(List.of(user1));

        try (MockedStatic<com.example.demo.util.AuthUtil> mock = mockStatic(com.example.demo.util.AuthUtil.class)) {
            mock.when(com.example.demo.util.AuthUtil::userFromSecurityContext).thenReturn("SOMEONE");

            when(dailyRepo.findTop100ByEmployeeIdOrderByStartDayDesc(anyInt())).thenReturn(List.of());
            when(mapper.toDailyDtoList(anyList())).thenReturn(List.of());
            when(mapper.toWeeklyDtoList(anyList())).thenReturn(List.of());
            when(mapper.toMonthlyDtoList(anyList())).thenReturn(List.of());
            when(mapper.toYearlyDtoList(anyList())).thenReturn(List.of());

            // Act
            StatisticsDto result = service.getStatistics();

            // Assert
            assertThat(result.getEmployees()).isEmpty();
        }
    }
}
