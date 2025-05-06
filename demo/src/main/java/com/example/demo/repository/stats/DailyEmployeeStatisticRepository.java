package com.example.demo.repository.stats;

import com.example.demo.entity.stats.EmployeeDailyStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DailyEmployeeStatisticRepository extends JpaRepository<EmployeeDailyStatsEntity, Integer> {

    List<EmployeeDailyStatsEntity> findAllByStartDayBetween(LocalDate from, LocalDate to);
    List<EmployeeDailyStatsEntity> findTop100ByEmployeeIdOrderByStartDayDesc(Integer employeeId);
}
