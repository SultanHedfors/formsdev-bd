package com.example.demo.repository.stats;

import com.example.demo.entity.stats.EmployeeMonthlyStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MonthlyEmployeeStatisticRepository extends JpaRepository<EmployeeMonthlyStatsEntity, Integer> {

    List<EmployeeMonthlyStatsEntity> findTop24ByEmployeeIdOrderByMonthDesc(Integer employeeId);

    void deleteByMonth(String month);
}
