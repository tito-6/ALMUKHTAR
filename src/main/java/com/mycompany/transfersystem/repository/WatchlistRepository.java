package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.Watchlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WatchlistRepository extends JpaRepository<Watchlist, Long> {

    List<Watchlist> findByUser_Id(Long userId);
}
