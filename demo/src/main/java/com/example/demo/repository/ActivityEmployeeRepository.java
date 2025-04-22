package com.example.demo.repository;

import com.example.demo.entity.ActivityEmployeeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface ActivityEmployeeRepository extends JpaRepository<ActivityEmployeeEntity, Integer> {

    @Query("SELECT CASE WHEN COUNT(ae) > 0 THEN true ELSE false END " +
            "FROM ActivityEmployeeEntity ae " +
            "WHERE ae.activity.activityId = :activityId AND ae.employee.id = :employeeId")
    boolean existsByActivityAndEmployee(@Param("activityId") Integer activityId,
                                        @Param("employeeId") Integer employeeId);
    void deleteByActivityActivityId(Integer activityId);

    List<ActivityEmployeeEntity> findByActivityActivityIdIn(Collection<Integer> activityIds);

    List<ActivityEmployeeEntity> findByActivityActivityId(Integer activityId);


}
