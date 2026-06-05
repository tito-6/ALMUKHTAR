package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.UserNotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, Long> {

    Optional<UserNotificationPreference> findByUserId(Long userId);
}
