package com.mycompany.transfersystem.service.ai;

import com.mycompany.transfersystem.entity.Transaction;
import com.mycompany.transfersystem.repository.AuditLogRepository;
import com.mycompany.transfersystem.repository.TransactionRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@ConditionalOnBean(VectorStore.class)
public class FinancialDataIngestionService {

    private final VectorStore vectorStore;
    private final TransactionRepository transactionRepository;
    private final AuditLogRepository auditLogRepository;

    public FinancialDataIngestionService(VectorStore vectorStore,
                                         TransactionRepository transactionRepository,
                                         AuditLogRepository auditLogRepository) {
        this.vectorStore = vectorStore;
        this.transactionRepository = transactionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Scheduled(cron = "0 0 2 * * *")
    @Transactional(readOnly = true)
    public void ingestRecentData() {
        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Transaction> transactions = transactionRepository.findByCreatedAtAfter(since);

        List<Document> docs = transactions.stream()
                .map(tx -> {
                    String currency = tx.getCurrencyCode() != null ? tx.getCurrencyCode() : "USD";
                    String content = String.format(
                            "Transaction #%d: %s sent %.2f %s to %s on %s. Status: %s. Fund: %s.",
                            tx.getId(),
                            tx.getSender().getUsername(),
                            tx.getAmount(),
                            currency,
                            tx.getReceiver().getUsername(),
                            tx.getCreatedAt().toString(),
                            tx.getStatus(),
                            tx.getFund().getName());
                    return new Document(content, Map.of(
                            "type", "TRANSACTION",
                            "userId", tx.getSender().getId().toString(),
                            "txId", tx.getId().toString(),
                            "date", tx.getCreatedAt().toString()));
                })
                .collect(Collectors.toList());

        if (!docs.isEmpty()) {
            vectorStore.add(docs);
        }
    }
}
