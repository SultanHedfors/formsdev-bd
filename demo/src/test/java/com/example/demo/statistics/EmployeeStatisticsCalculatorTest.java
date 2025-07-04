package com.example.demo.statistics;

import com.example.demo.entity.ActivityEmployeeEntity;
import com.example.demo.entity.stats.EmployeeDailyStatsEntity;
import com.example.demo.entity.stats.EmployeeMonthlyStatsEntity;
import com.example.demo.entity.stats.EmployeeWeeklyStatsEntity;
import com.example.demo.entity.stats.EmployeeYearlyStatsEntity;
import com.example.demo.repository.ActivityEmployeeRepository;
import com.example.demo.repository.stats.DailyEmployeeStatisticRepository;
import com.example.demo.repository.stats.MonthlyEmployeeStatisticRepository;
import com.example.demo.repository.stats.WeeklyEmployeeStatisticRepository;
import com.example.demo.repository.stats.YearlyEmployeeStatisticRepository;
import com.example.demo.service.EmployeeStatisticsCalculator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.Year;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;

import static com.example.demo.statistics.EmployeeStatisticsCalculatorTestHelper.createActivityEmployees;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
@Slf4j
class EmployeeStatisticsCalculatorTest {

    @Mock
    ActivityEmployeeRepository activityRepo;
    @Mock
    DailyEmployeeStatisticRepository dailyRepo;
    @Mock
    WeeklyEmployeeStatisticRepository weeklyRepo;
    @Mock
    MonthlyEmployeeStatisticRepository monthlyRepo;
    @Mock
    YearlyEmployeeStatisticRepository yearlyRepo;

    @Captor
    ArgumentCaptor<EmployeeDailyStatsEntity> statsCaptor;
    @Captor
    ArgumentCaptor<EmployeeWeeklyStatsEntity> weeklyStatsCaptor;
    @Captor
    ArgumentCaptor<EmployeeMonthlyStatsEntity> monthlyStatsCaptor;
    @Captor
    ArgumentCaptor<EmployeeYearlyStatsEntity> yearlyStatsCaptor;

    EmployeeStatisticsCalculator statisticsCalculator;

    // Common test params
    Map<Integer, Double> expectedScores = Map.of(
            1, 0.9375,
            2, 0.8666666666666667,
            3, 0.9130434782608695,
            4, 0.8
    );
    LocalDate testDate = LocalDate.of(2024, 7, 10);
    LocalDate monday = LocalDate.of(2024, 7, 8);
    YearMonth testMonth = YearMonth.of(2024, 7);
    int testYear = 2024;
    List<ActivityEmployeeEntity> activityEmployees = createActivityEmployees();

    @BeforeEach
    void setUp() {
        statisticsCalculator = new EmployeeStatisticsCalculator(activityRepo, dailyRepo, weeklyRepo, monthlyRepo, yearlyRepo);
        when(activityRepo.findWithGraphByActivityDate(any(), any())).thenReturn(activityEmployees);
    }

    @Test
    void calculateDailyScores() {
        statisticsCalculator.calculateDailyScores(testDate);

        verify(dailyRepo, atLeastOnce()).save(statsCaptor.capture());
        var savedEntities = statsCaptor.getAllValues();
        assertThat(savedEntities).isNotEmpty();

        savedEntities.forEach(entity -> assertStats(entity.getEmployeeId(), entity.getScore(), expectedScores.get(entity.getEmployeeId()), entity.getStartDay(), testDate, entity.getUpdatedAt()));
    }

    @Test
    void calculateWeeklyScores() {
        statisticsCalculator.calculateWeeklyScores(monday);

        verify(weeklyRepo, atLeastOnce()).save(weeklyStatsCaptor.capture());
        var savedEntities = weeklyStatsCaptor.getAllValues();

        savedEntities.forEach(entity -> assertStats(entity.getEmployeeId(), entity.getScore(), expectedScores.get(entity.getEmployeeId()), entity.getWeekStart(), monday, entity.getUpdatedAt()));
    }

    @Test
    void calculateMonthlyScores() {
        statisticsCalculator.calculateMonthlyScores(testMonth);

        verify(monthlyRepo, atLeastOnce()).save(monthlyStatsCaptor.capture());
        var savedEntities = monthlyStatsCaptor.getAllValues();

        savedEntities.forEach(entity -> assertStats(entity.getEmployeeId(), entity.getScore(), expectedScores.get(entity.getEmployeeId()), entity.getMonth(), testMonth.toString(), entity.getUpdatedAt()));
    }

    @Test
    void calculateYearlyScores() {
        statisticsCalculator.calculateYearlyScores(Year.of(testYear));

        verify(yearlyRepo, atLeastOnce()).save(yearlyStatsCaptor.capture());
        var savedEntities = yearlyStatsCaptor.getAllValues();

        savedEntities.forEach(entity -> assertStats(entity.getEmployeeId(), entity.getScore(), expectedScores.get(entity.getEmployeeId()), entity.getYear(), testYear, entity.getUpdatedAt()));
    }

    //    common assertions
    private <T> void assertStats(int empId, double actual, double expected, T actualPeriod, T expectedPeriod, Object updatedAt) {
        assertTrue(expectedScores.containsKey(empId), "Unexpected employeeId: " + empId);
        assertEquals(expected, actual);
        assertEquals(expectedPeriod, actualPeriod);
        assertNotNull(updatedAt);
    }
}
