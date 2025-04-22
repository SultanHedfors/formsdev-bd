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








}
