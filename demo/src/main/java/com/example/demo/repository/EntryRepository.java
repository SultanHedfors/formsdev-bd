package com.example.demo.repository;

import com.example.demo.entity.EntryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface EntryRepository extends JpaRepository<EntryEntity, Long> {

}
