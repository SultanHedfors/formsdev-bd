package com.example.demo.repository;

import com.example.demo.entity.ActivityAssignmentLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityAssignmentLogRepository extends JpaRepository<ActivityAssignmentLogEntity, Integer> {

    Optional<ActivityAssignmentLogEntity> findTopByActivity_ActivityIdOrderByAssignedAtDesc(Integer activityId);

    @Query("SELECT DISTINCT a.activity.activityId FROM ActivityAssignmentLogEntity a WHERE a.activity.activityId IN :activityIds")
    List<Integer> findExistingActivityIdsInLog(@Param("activityIds") List<Integer> activityIds);

    List<ActivityAssignmentLogEntity> findByActivity_ActivityIdOrderByAssignedAtDesc(Integer activityId);

}
