package com.mycompany.transfersystem.service.bills;

import com.mycompany.transfersystem.entity.BillProvider;
import com.mycompany.transfersystem.repository.BillProviderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class BillProviderService {

    private final BillProviderRepository billProviderRepository;

    public BillProviderService(BillProviderRepository billProviderRepository) {
        this.billProviderRepository = billProviderRepository;
    }

    @Transactional(readOnly = true)
    public List<BillProvider> listActive() {
        return billProviderRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<BillProvider> listByCategory(String category) {
        if (category == null || category.isBlank()) {
            return billProviderRepository.findByActiveTrue();
        }
        return billProviderRepository.findByCategoryAndActiveTrue(category);
    }

    @Transactional(readOnly = true)
    public BillProvider getById(Long id) {
        return billProviderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bill provider not found: " + id));
    }

    @Transactional
    public BillProvider create(BillProvider provider) {
        return billProviderRepository.save(provider);
    }
}
