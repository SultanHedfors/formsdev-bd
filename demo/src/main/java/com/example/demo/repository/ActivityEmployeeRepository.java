package com.example.demo.repository;

import com.example.demo.entity.ActivityEmployeeEntity;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

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
    int deleteAllByActivityDateRangeAndUserModifiedFalse(@Param("from") LocalDateTime from,
                                                         @Param("to") LocalDateTime to);

    @EntityGraph(attributePaths = {
            "activity",
            "activity.procedure",
            "workSchedule",
            "activity.employee"
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
