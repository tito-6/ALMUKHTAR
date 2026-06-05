package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.VaultDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VaultDocumentRepository extends JpaRepository<VaultDocument, Long> {
    List<VaultDocument> findByOwnerUserId(Long userId);
}
