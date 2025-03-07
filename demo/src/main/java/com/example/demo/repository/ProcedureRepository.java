package com.example.demo.repository;

import com.example.demo.entity.ProcedureEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcedureRepository extends JpaRepository<ProcedureEntity, Integer> {
}
