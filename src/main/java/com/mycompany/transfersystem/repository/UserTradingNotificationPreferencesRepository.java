package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.UserTradingNotificationPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserTradingNotificationPreferencesRepository extends JpaRepository<UserTradingNotificationPreferences, Long> {

    Optional<UserTradingNotificationPreferences> findByUserId(Long userId);
}
