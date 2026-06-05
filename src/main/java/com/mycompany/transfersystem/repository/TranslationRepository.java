package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.Translation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TranslationRepository extends JpaRepository<Translation, Long> {
    Optional<Translation> findByLocaleAndMessageKey(String locale, String messageKey);
    List<Translation> findAllByLocale(String locale);
}
