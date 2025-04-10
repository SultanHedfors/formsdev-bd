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
import java.util.Optional;

@Repository
public interface ActivityRepository extends JpaRepository<ActivityEntity, Integer> {

    Page<ActivityEntity> findByActivityDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);


    @Query("SELECT a FROM ActivityEntity a WHERE a.employee IS NULL " +
            "AND EXTRACT(YEAR FROM a.activityDate) = :year " +
            "AND EXTRACT(MONTH FROM a.activityDate) = :month " +
            "AND EXTRACT(DAY FROM a.activityDate) = :day " +
            "AND EXTRACT(HOUR FROM a.activityTime) = :hour " +
            "AND EXTRACT(MINUTE FROM a.activityTime) = :minute " +
            "AND EXTRACT(SECOND FROM a.activityTime) = :second " +
            "AND NOT EXISTS (" +
            "   SELECT ws FROM WorkSchedule ws WHERE ws.activity = a" +
            ") " +
            "ORDER BY a.activityDate ASC")
    Optional<ActivityEntity> findFirstActivityWithoutScheduleByDateTime(
            @Param("year") int year,
            @Param("month") int month,
            @Param("day") int day,
            @Param("hour") int hour,
            @Param("minute") int minute,
            @Param("second") int second);




}
