package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.PrepaidCard;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrepaidCardRepository extends JpaRepository<PrepaidCard, Long> {

    List<PrepaidCard> findByUser_Id(Long userId);
}
