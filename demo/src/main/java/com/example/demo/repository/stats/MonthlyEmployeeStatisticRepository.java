package com.example.demo.repository.stats;

import com.example.demo.entity.stats.EmployeeMonthlyStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MonthlyEmployeeStatisticRepository extends JpaRepository<EmployeeMonthlyStatsEntity, Integer> {

    List<EmployeeMonthlyStatsEntity> findTop24ByEmployeeIdOrderByMonthDesc(Integer employeeId);
}
