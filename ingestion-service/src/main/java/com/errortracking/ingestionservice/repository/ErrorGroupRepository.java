package com.errortracking.ingestionservice.repository;

import com.errortracking.ingestionservice.entity.ErrorGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ErrorGroupRepository extends JpaRepository<ErrorGroup, String> {
}