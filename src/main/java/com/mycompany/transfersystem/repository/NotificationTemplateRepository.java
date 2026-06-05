package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.NotificationTemplate;
import com.mycompany.transfersystem.entity.enums.NotificationChannel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, Long> {

    Optional<NotificationTemplate> findByTemplateKeyAndLocaleIgnoreCaseAndChannel(
            String templateKey, String locale, NotificationChannel channel);

    List<NotificationTemplate> findByTemplateKeyAndChannel(String templateKey, NotificationChannel channel);
}
