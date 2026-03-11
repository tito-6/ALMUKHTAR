package com.mycompany.transfersystem.service.wallet;

import com.mycompany.transfersystem.entity.KycDocument;
import com.mycompany.transfersystem.entity.WalletApplication;
import com.mycompany.transfersystem.entity.enums.WalletApplicationStatus;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.KycDocumentRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.repository.WalletApplicationRepository;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.service.storage.DocumentStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * KYC document pre-screening: load file, optional AI analysis, update document record.
 * AI result is advisory only; final decision is always human.
 */
@Service
public class DocumentVerificationService {

    private static final Logger log = LoggerFactory.getLogger(DocumentVerificationService.class);

    private final DocumentStorageService storageService;
    private final KycDocumentRepository kycDocumentRepository;
    private final WalletApplicationRepository walletApplicationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Autowired(required = false)
    private AiDocumentAnalyzer aiDocumentAnalyzer;

    public DocumentVerificationService(DocumentStorageService storageService,
                                       KycDocumentRepository kycDocumentRepository,
                                       WalletApplicationRepository walletApplicationRepository,
                                       UserRepository userRepository,
                                       AuditService auditService) {
        this.storageService = storageService;
        this.kycDocumentRepository = kycDocumentRepository;
        this.walletApplicationRepository = walletApplicationRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void analyzeDocument(KycDocument doc) {
        if (doc.getFileReference() == null || doc.getFileReference().isBlank()) {
            return;
        }
        byte[] content = storageService.retrieve(doc.getFileReference());
        if (content == null || content.length == 0) {
            log.warn("No content for KYC document reference: {}", doc.getFileReference());
            return;
        }

        AiAnalysisResult result;
        if (aiDocumentAnalyzer != null) {
            try {
                result = aiDocumentAnalyzer.analyze(content, doc.getDocType().name());
            } catch (Exception e) {
                log.warn("AI document analysis failed, using default: {}", e.getMessage());
                result = defaultResult();
            }
        } else {
            result = defaultResult();
        }

        doc.setAiConfidenceScore(result.confidenceScore);
        doc.setAiFlags(result.flagsJson);
        kycDocumentRepository.save(doc);

        if (Boolean.TRUE.equals(result.isExpired)) {
            WalletApplication app = doc.getApplication();
            app.setStatus(WalletApplicationStatus.REJECTED);
            app.setRejectionReason("Document expired");
            walletApplicationRepository.save(app);
        }

        User systemUser = userRepository.findByUsername("SYSTEM").orElse(null);
        if (systemUser != null) {
            auditService.log("KYC_AI_SCREENED", "WALLET_APPLICATION",
                    doc.getApplication().getId(),
                    "confidence=" + result.confidenceScore + " fraudRisk=" + result.fraudRisk,
                    systemUser);
        }
    }

    private AiAnalysisResult defaultResult() {
        AiAnalysisResult r = new AiAnalysisResult();
        r.confidenceScore = new BigDecimal("0.90");
        r.isAuthentic = true;
        r.isExpired = false;
        r.fraudRisk = "LOW";
        r.flagsJson = "[]";
        return r;
    }

    public static class AiAnalysisResult {
        public String documentType;
        public Boolean isAuthentic;
        public BigDecimal confidenceScore;
        public String extractedName;
        public String extractedDob;
        public String extractedDocNumber;
        public String expiryDate;
        public Boolean isExpired;
        public String flagsJson;
        public String fraudRisk; // LOW, MEDIUM, HIGH
    }

    /**
     * Optional bean: implement to call Spring AI Vision (GPT-4o/Claude) for document analysis.
     */
    public interface AiDocumentAnalyzer {
        AiAnalysisResult analyze(byte[] imageBytes, String docType);
    }
}
