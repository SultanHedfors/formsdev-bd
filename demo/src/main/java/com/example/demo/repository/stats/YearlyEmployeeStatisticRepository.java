package com.example.demo.repository.stats;


import com.example.demo.entity.stats.EmployeeYearlyStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Year;
import java.util.List;
import java.util.Optional;

public interface YearlyEmployeeStatisticRepository extends JpaRepository<EmployeeYearlyStatsEntity, Integer> {
    Optional<EmployeeYearlyStatsEntity> findByEmployeeIdAndYear(Integer employeeId, Integer year);

    List<EmployeeYearlyStatsEntity> findTop5ByEmployeeIdOrderByYearDesc(Integer employeeId);
}
