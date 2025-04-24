package com.example.demo.repository.stats;

import com.example.demo.entity.stats.EmployeeMonthlyStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.YearMonth;
import java.util.Optional;

public interface MonthlyEmployeeStatisticRepository extends JpaRepository<EmployeeMonthlyStatsEntity, Integer> {
    Optional<EmployeeMonthlyStatsEntity> findByEmployeeIdAndMonth(Integer employeeId, String month);

}
