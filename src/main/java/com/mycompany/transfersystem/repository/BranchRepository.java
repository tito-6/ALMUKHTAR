package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.Branch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findFirstByName(String name);
}