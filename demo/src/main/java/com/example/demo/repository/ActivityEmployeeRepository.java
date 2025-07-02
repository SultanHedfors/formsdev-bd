package com.example.demo.repository;

import com.example.demo.entity.ActivityEmployeeEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ActivityEmployeeRepository extends JpaRepository<ActivityEmployeeEntity, Integer> {


    void deleteByActivityActivityId(Integer activityId);

    List<ActivityEmployeeEntity> findByActivityActivityIdIn(Collection<Integer> activityIds);

    List<ActivityEmployeeEntity> findByActivityActivityId(Integer activityId);

    @Modifying
    @Query("""
                DELETE FROM ActivityEmployeeEntity ae
                WHERE ae.userModified = false
                AND ae.activity.activityDate BETWEEN :from AND :to
            """)
    int deleteAllByActivityDateRangeAndUserModifiedFalse(@Param("from") Timestamp from,
                                                         @Param("to") Timestamp to);

    @Query(value = """
            SELECT DISTINCT activity_id
            FROM activity_employee
            WHERE user_modified = 1
            AND activity_id IN (:ids)
            """, nativeQuery = true)
    List<Integer> findManualModifiedActivityIds(@Param("ids") List<Integer> activityIds);

    @Query(value = """
            SELECT CAST(activity_id AS VARCHAR(20)) || ':' ||
            CAST(employee_id AS VARCHAR(20))
            FROM activity_employee
            """,
            nativeQuery = true)
    Set<String> findAllExistingActivityEmployeePairs();

    @EntityGraph(attributePaths = {
            "activity", "activity.procedure", "workSchedule"
    })
    @Query("""
                SELECT ae FROM ActivityEmployeeEntity ae
                WHERE ae.activity.activityDate >= :startOfDay
                  AND ae.activity.activityDate < :endOfDay
            """)
    List<ActivityEmployeeEntity> findWithGraphByActivityDate(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay
    );


}
