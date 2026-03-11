package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.dto.*;
import com.mycompany.transfersystem.entity.CorporateAccount;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.CorporateAccountRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CorporateAccountService {

    private final CorporateAccountRepository corporateAccountRepository;
    private final UserRepository userRepository;
    private final TransactionService transactionService;

    public CorporateAccountService(CorporateAccountRepository corporateAccountRepository,
                                   UserRepository userRepository,
                                   TransactionService transactionService) {
        this.corporateAccountRepository = corporateAccountRepository;
        this.userRepository = userRepository;
        this.transactionService = transactionService;
    }

    @Transactional
    public CorporateAccountResponse createSubAccount(CreateSubAccountRequest request, User actor) {
        User parentUser = userRepository.findById(request.getParentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent user not found"));
        User subUser = request.getSubUserId() != null
                ? userRepository.findById(request.getSubUserId()).orElse(null)
                : null;

        CorporateAccount account = CorporateAccount.builder()
                .parentUser(parentUser)
                .subUser(subUser)
                .accountLabel(request.getAccountLabel())
                .spendingLimit(request.getSpendingLimit())
                .active(true)
                .build();
        account = corporateAccountRepository.save(account);
        return toResponse(account);
    }

    public Page<CorporateAccountResponse> listSubAccounts(Long parentUserId, Pageable pageable) {
        return corporateAccountRepository.findByParentUser_Id(parentUserId, pageable)
                .map(this::toResponse);
    }

    @Transactional
    public PayrollBatchResponse processPayrollBatch(Long parentUserId, PayrollBatchRequest request, User actor) {
        User parent = userRepository.findById(parentUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Parent user not found"));
        int success = 0, failed = 0;
        for (PayrollItem item : request.getItems()) {
            try {
                TransferRequest tr = new TransferRequest();
                tr.setSenderId(parentUserId);
                tr.setReceiverId(item.getReceiverId());
                tr.setFundId(item.getFundId());
                tr.setAmount(item.getAmount());
                transactionService.createTransfer(tr);
                success++;
            } catch (Exception e) {
                failed++;
            }
        }
        return PayrollBatchResponse.builder()
                .successCount(success)
                .failedCount(failed)
                .build();
    }

    public LedgerSummaryResponse getLedgerSummary(Long parentUserId) {
        return LedgerSummaryResponse.builder()
                .parentUserId(parentUserId)
                .subAccountCount(corporateAccountRepository.findByParentUser_Id(parentUserId, Pageable.unpaged()).getTotalElements())
                .build();
    }

    private CorporateAccountResponse toResponse(CorporateAccount a) {
        return CorporateAccountResponse.builder()
                .id(a.getId())
                .parentUserId(a.getParentUser().getId())
                .subUserId(a.getSubUser() != null ? a.getSubUser().getId() : null)
                .accountLabel(a.getAccountLabel())
                .spendingLimit(a.getSpendingLimit())
                .active(a.isActive())
                .build();
    }
}
