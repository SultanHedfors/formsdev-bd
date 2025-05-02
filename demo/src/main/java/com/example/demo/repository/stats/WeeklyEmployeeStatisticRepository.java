package com.example.demo.repository.stats;

import com.example.demo.entity.stats.EmployeeWeeklyStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WeeklyEmployeeStatisticRepository extends JpaRepository<EmployeeWeeklyStatsEntity, Integer> {
    Optional<EmployeeWeeklyStatsEntity> findByEmployeeIdAndWeekStart(Integer employeeId, LocalDate weekStart);

    List<EmployeeWeeklyStatsEntity> findTop36ByEmployeeIdOrderByWeekStartDesc(Integer employeeId);
}
