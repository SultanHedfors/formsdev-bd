package com.example.demo.repository.stats;

import com.example.demo.entity.stats.EmployeeDailyStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyEmployeeStatisticRepository extends JpaRepository<EmployeeDailyStatsEntity, Integer> {

    @Transactional
    void deleteByStartDayBetween(LocalDate start, LocalDate end);

    List<EmployeeDailyStatsEntity> findAllByStartDayBetween(LocalDate from, LocalDate to);
    List<EmployeeDailyStatsEntity> findTop100ByEmployeeIdOrderByStartDayDesc(Integer employeeId);
}
