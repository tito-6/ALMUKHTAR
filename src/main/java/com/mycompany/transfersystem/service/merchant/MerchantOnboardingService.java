package com.mycompany.transfersystem.service.merchant;

import com.mycompany.transfersystem.entity.Branch;
import com.mycompany.transfersystem.entity.Merchant;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.BranchRepository;
import com.mycompany.transfersystem.repository.MerchantRepository;
import com.mycompany.transfersystem.service.AuditService;
import com.mycompany.transfersystem.util.QrEncryptionUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class MerchantOnboardingService {

    private static final String QR_PREFIX = "MERCHANT|";

    private final MerchantRepository merchantRepository;
    private final BranchRepository branchRepository;
    private final QrEncryptionUtil qrEncryptionUtil;
    private final AuditService auditService;

    public MerchantOnboardingService(MerchantRepository merchantRepository,
                                     BranchRepository branchRepository,
                                     QrEncryptionUtil qrEncryptionUtil,
                                     AuditService auditService) {
        this.merchantRepository = merchantRepository;
        this.branchRepository = branchRepository;
        this.qrEncryptionUtil = qrEncryptionUtil;
        this.auditService = auditService;
    }

    @Transactional
    public Merchant register(User owner, String businessName, String category, String registrationNumber,
                            String address, String city, String country, Long branchId) {
        if (merchantRepository.findByOwnerUser_Id(owner.getId()).isPresent()) {
            throw new IllegalStateException("User already has a merchant account");
        }
        Branch branch = branchId != null ? branchRepository.findById(branchId).orElse(null) : null;
        Merchant m = Merchant.builder()
                .ownerUser(owner)
                .businessName(businessName)
                .category(category)
                .registrationNumber(registrationNumber)
                .address(address)
                .city(city)
                .country(country)
                .branch(branch)
                .status("PENDING")
                .kycApproved(false)
                .build();
        m = merchantRepository.save(m);
        auditService.log("MERCHANT_REGISTERED", "MERCHANT", m.getId(), "businessName=" + businessName, owner);
        return m;
    }

    @Transactional
    public void generateQr(Merchant merchant) {
        String payload = QR_PREFIX + merchant.getId();
        String encrypted = qrEncryptionUtil.encrypt(payload);
        merchant.setQrCodeData(encrypted);
        merchant.setQrCodeImagePath(null);
        merchantRepository.save(merchant);
    }

    @Transactional(readOnly = true)
    public Merchant getByOwnerUserId(Long ownerUserId) {
        return merchantRepository.findByOwnerUser_Id(ownerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found for user"));
    }

    @Transactional(readOnly = true)
    public Merchant getById(Long id) {
        return merchantRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant not found: " + id));
    }

    @Transactional
    public Merchant approve(Long merchantId, User admin) {
        Merchant m = getById(merchantId);
        m.setStatus("ACTIVE");
        m.setKycApproved(true);
        m.setUpdatedAt(java.time.Instant.now());
        if (m.getQrCodeData() == null) {
            generateQr(m);
        }
        m = merchantRepository.save(m);
        auditService.log("MERCHANT_APPROVED", "MERCHANT", m.getId(), "Activated by admin", admin);
        return m;
    }

    @Transactional(readOnly = true)
    public List<Merchant> listByStatus(String status) {
        if (status == null || status.isBlank()) {
            return merchantRepository.findAll();
        }
        return merchantRepository.findByStatus(status);
    }

    public Long decodeMerchantIdFromQr(String qrData) {
        String decrypted = qrEncryptionUtil.decrypt(qrData);
        if (!decrypted.startsWith(QR_PREFIX)) {
            throw new IllegalArgumentException("Invalid merchant QR payload");
        }
        String idStr = decrypted.substring(QR_PREFIX.length()).trim();
        return Long.parseLong(idStr);
    }
}
