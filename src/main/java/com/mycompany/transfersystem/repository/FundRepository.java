package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.Fund;
import com.mycompany.transfersystem.entity.enums.FundStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FundRepository extends JpaRepository<Fund, Long> {
    List<Fund> findByStatus(FundStatus status);
    boolean existsByName(String name);
    Optional<Fund> findByName(String name);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Fund f where f.id = :id")
    Optional<Fund> findByIdForUpdate(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Fund f where f.name = :name")
    Optional<Fund> findByNameForUpdate(@Param("name") String name);
}
