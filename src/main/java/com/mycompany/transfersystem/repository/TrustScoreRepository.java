package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.TrustScore;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrustScoreRepository extends JpaRepository<TrustScore, Long> {
    Optional<TrustScore> findByUser_Id(Long userId);
    Page<TrustScore> findAllByOrderByScoreDesc(Pageable pageable);
}
