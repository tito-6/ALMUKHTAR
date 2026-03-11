package com.mycompany.transfersystem.service.lending;

import com.mycompany.transfersystem.entity.Loan;
import com.mycompany.transfersystem.repository.LoanRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Default risk monitoring, restructuring logic, debt recovery escalation.
 * Stub for MVP; extend with full escalation and restructuring workflows.
 */
@Service
public class LoanRiskService {

    private final LoanRepository loanRepository;

    public LoanRiskService(LoanRepository loanRepository) {
        this.loanRepository = loanRepository;
    }

    @Transactional(readOnly = true)
    public List<Loan> getAtRiskLoans() {
        return loanRepository.findByStatus("ACTIVE");
    }
}
