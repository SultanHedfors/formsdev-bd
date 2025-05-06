package com.example.demo.repository.stats;


import com.example.demo.entity.stats.EmployeeYearlyStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface YearlyEmployeeStatisticRepository extends JpaRepository<EmployeeYearlyStatsEntity, Integer> {
    List<EmployeeYearlyStatsEntity> findTop5ByEmployeeIdOrderByYearDesc(Integer employeeId);
}
