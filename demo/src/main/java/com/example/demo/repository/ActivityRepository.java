package com.example.demo.repository;

import com.example.demo.entity.ActivityEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ActivityRepository extends JpaRepository<ActivityEntity, Integer> {

    Page<ActivityEntity> findByActivityDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query(value = """
    SELECT a FROM ActivityEntity a
    JOIN FETCH a.procedure
    WHERE CAST(a.activityDate AS date) = :date
    """)
    List<ActivityEntity> findByActivityDateWithProcedure(@Param("date") java.sql.Date date);

    @Query("""
SELECT a FROM ActivityEntity a
LEFT JOIN FETCH a.procedure p
LEFT JOIN FETCH a.room r
WHERE a.activityDate BETWEEN :start AND :end
""")
    List<ActivityEntity> findActivitiesInDateRange(@Param("start") Timestamp start, @Param("end") Timestamp end);



    @Query("""
    SELECT a FROM ActivityEntity a
    WHERE a.activityDate BETWEEN :startOfDay AND :endOfDay
      AND a.activityTime BETWEEN :startTime AND :endTime
      AND a.procedure IS NOT NULL
""")
    List<ActivityEntity> findActivitiesInFullRange(
            @Param("startOfDay") Timestamp startOfDay,
            @Param("endOfDay") Timestamp endOfDay,
            @Param("startTime") Timestamp startTime,
            @Param("endTime") Timestamp endTime
    );
    @Query("""
SELECT a FROM ActivityEntity a
WHERE a.activityDate BETWEEN :start AND :end
AND a.procedure IS NOT NULL
""")
    List<ActivityEntity> findActivitiesInDateRangeWithProcedure(
            @Param("start") Timestamp start,
            @Param("end") Timestamp end
    );
    // Zwraca ID aktywności, które mają przypisanych pracowników z flagą user_modified = true








}
