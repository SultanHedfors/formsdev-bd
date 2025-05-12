package com.example.demo.repository.stats;


import com.example.demo.entity.stats.EmployeeYearlyStatsEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface YearlyEmployeeStatisticRepository extends JpaRepository<EmployeeYearlyStatsEntity, Integer> {


    // Usuwanie rekordów na podstawie roku
    @Transactional
    void deleteByYear(Integer year); // Usuwamy wszystkie rekordy z danego roku
    List<EmployeeYearlyStatsEntity> findTop5ByEmployeeIdOrderByYearDesc(Integer employeeId);
}
