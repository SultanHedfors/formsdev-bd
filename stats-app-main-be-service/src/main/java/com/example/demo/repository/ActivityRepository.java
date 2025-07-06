package com.example.demo.repository;

import com.example.demo.entity.ActivityEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface ActivityRepository extends JpaRepository<ActivityEntity, Integer> {

    Page<ActivityEntity> findByActivityDateBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

}
