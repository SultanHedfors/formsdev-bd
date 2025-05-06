package com.example.demo.repository;

import com.example.demo.entity.WorkSchedule;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<WorkSchedule, Integer> {
    @Transactional
    void deleteByYearMonth(String yearMonth);
    boolean existsByEmployee_IdAndYearMonthAndDayOfMonth(Integer employeeId, String yearMonth, Integer dayOfMonth);

    @Query("SELECT ws.id FROM WorkSchedule ws WHERE ws.processed IS NULL OR ws.processed = false")
    List<Integer> findIdsOfUnprocessedSchedules();

    // Zapytanie filtrujące po 'yearMonth'
    List<WorkSchedule> findByYearMonthBetween(String startYearMonth, String endYearMonth);







}
