package com.example.demo.repository;

import com.example.demo.entity.ActivityEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ActivityRepository extends JpaRepository<ActivityEntity, Integer> {

    Page<ActivityEntity> findByActivityDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Query("""
            SELECT a FROM ActivityEntity a
            WHERE a.activityDate BETWEEN :start AND :end
            AND a.procedure IS NOT NULL
            """)
    List<ActivityEntity> findActivitiesInDateRangeWithProcedure(
            @Param("start") Timestamp start,
            @Param("end") Timestamp end
    );



}
