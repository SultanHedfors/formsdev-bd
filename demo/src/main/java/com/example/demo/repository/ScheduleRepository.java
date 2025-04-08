package com.example.demo.repository;

import com.example.demo.entity.WorkSchedule;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface ScheduleRepository extends JpaRepository<WorkSchedule, Integer> {
    @Transactional
    void deleteByYearMonth(String yearMonth);
    boolean existsByEmployee_IdAndYearMonthAndDayOfMonth(Integer employeeId, String yearMonth, Integer dayOfMonth);

    @Query(value = "SELECT * FROM work_schedule ws " +
            "WHERE (ws.year_month || '-' || " +
            "CASE WHEN ws.day_of_month < 10 THEN '0' || CAST(ws.day_of_month AS varchar(2)) " +
            "ELSE CAST(ws.day_of_month AS varchar(2)) END) IN (:dates)",
            nativeQuery = true)
    List<WorkSchedule> findByDateIn(@Param("dates") Set<String> dates);


    @Query(value = "SELECT MIN(ws.YEAR_MONTH || '-' || LPAD(ws.DAY_OF_MONTH, 2, '0')) FROM WORK_SCHEDULE ws", nativeQuery = true)
    String findEarliestScheduleDate();

}
