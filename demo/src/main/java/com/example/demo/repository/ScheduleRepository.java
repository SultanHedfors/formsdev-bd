package com.example.demo.repository;

import com.example.demo.entity.WorkSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScheduleRepository extends JpaRepository<WorkSchedule, Long> {

}
