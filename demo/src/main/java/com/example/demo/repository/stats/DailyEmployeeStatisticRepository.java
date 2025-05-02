package com.example.demo.repository.stats;

import com.example.demo.entity.stats.EmployeeDailyStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyEmployeeStatisticRepository extends JpaRepository<EmployeeDailyStatsEntity, Integer> {
    Optional<EmployeeDailyStatsEntity> findByEmployeeIdAndStartDay(Integer employeeId, LocalDate startDay);

    List<EmployeeDailyStatsEntity> findTop100ByEmployeeIdOrderByStartDayDesc(Integer employeeId);
}
