package com.example.demo.repository;

import com.example.demo.entity.ActivityEmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ActivityEmployeeRepository extends JpaRepository<ActivityEmployeeEntity, Integer> {

    void deleteByActivityActivityIdInAndUserModifiedFalse(Set<Integer> activityIds);

    void deleteByActivityActivityId(Integer activityId);

    List<ActivityEmployeeEntity> findByActivityActivityIdIn(Collection<Integer> activityIds);

    List<ActivityEmployeeEntity> findByActivityActivityId(Integer activityId);

    List<ActivityEmployeeEntity> findByActivityActivityIdInAndUserModifiedTrue(Set<Integer> activityIds);
}
