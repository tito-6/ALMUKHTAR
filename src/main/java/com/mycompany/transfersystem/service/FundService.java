package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.dto.FundRequest;
import com.mycompany.transfersystem.dto.FundResponse;
import com.mycompany.transfersystem.entity.Fund;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.FundRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FundService {

    @Autowired
    private FundRepository fundRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditService auditService;

    public List<FundResponse> getAllFunds() {
        return fundRepository.findAll().stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public FundResponse getFundById(Long id) {
        Fund fund = fundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fund not found with id: " + id));
        return convertToResponse(fund);
    }

    public FundResponse createFund(FundRequest request) {
        Fund fund = new Fund();
        fund.setName(request.getName());
        fund.setBalance(request.getBalance());
        fund.setStatus(request.getStatus());

        Fund savedFund = fundRepository.save(fund);
        
        // Log the creation
        User currentUser = getCurrentUser();
        auditService.log("CREATE_FUND", currentUser, "Fund", savedFund.getId());

        return convertToResponse(savedFund);
    }

    public FundResponse updateFund(Long id, FundRequest request) {
        Fund fund = fundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fund not found with id: " + id));

        fund.setName(request.getName());
        fund.setBalance(request.getBalance());
        fund.setStatus(request.getStatus());

        Fund updatedFund = fundRepository.save(fund);
        
        // Log the update
        User currentUser = getCurrentUser();
        auditService.log("UPDATE_FUND", currentUser, "Fund", updatedFund.getId());

        return convertToResponse(updatedFund);
    }

    public void deleteFund(Long id) {
        Fund fund = fundRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fund not found with id: " + id));

        // Log the deletion
        User currentUser = getCurrentUser();
        auditService.log("DELETE_FUND", currentUser, "Fund", fund.getId());

        fundRepository.delete(fund);
    }

    private FundResponse convertToResponse(Fund fund) {
        return new FundResponse(
                fund.getId(),
                fund.getName(),
                fund.getBalance(),
                fund.getStatus(),
                fund.getCreatedAt(),
                fund.getUpdatedAt()
        );
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Current user not found"));
    }
}