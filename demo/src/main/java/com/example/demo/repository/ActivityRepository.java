package com.example.demo.repository;

import com.example.demo.entity.ActivityEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<ActivityEntity, Integer> {

    Page<ActivityEntity> findByActivityDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("SELECT a FROM ActivityEntity a " +
            "WHERE a.activityDate >= :minDate " +
            "AND NOT EXISTS (" +
            "SELECT ws FROM WorkSchedule ws WHERE ws.activity = a)")
    List<ActivityEntity> findActivitiesWithoutScheduleFromDate(@Param("minDate") LocalDateTime minDate);




}
