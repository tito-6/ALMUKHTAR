package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.KycDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KycDocumentRepository extends JpaRepository<KycDocument, Long> {

    List<KycDocument> findByApplicationIdOrderByUploadAtDesc(Long applicationId);
}
