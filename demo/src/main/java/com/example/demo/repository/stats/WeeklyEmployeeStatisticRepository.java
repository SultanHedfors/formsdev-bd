package com.example.demo.repository.stats;

import com.example.demo.entity.stats.EmployeeWeeklyStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface WeeklyEmployeeStatisticRepository extends JpaRepository<EmployeeWeeklyStatsEntity, Integer> {

    List<EmployeeWeeklyStatsEntity> findTop36ByEmployeeIdOrderByWeekStartDesc(Integer employeeId);

    @Transactional
    void deleteByWeekStartBetween(LocalDate start, LocalDate end); // Usuwamy rekordy w danym przedziale dat

}
