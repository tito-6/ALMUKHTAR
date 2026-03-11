package com.mycompany.transfersystem.service.batch;

import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.Wallet;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.repository.WalletBalanceRepository;
import com.mycompany.transfersystem.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

@Service
public class BatchValidationService {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final WalletBalanceRepository walletBalanceRepository;

    public BatchValidationService(UserRepository userRepository, WalletRepository walletRepository,
                                  WalletBalanceRepository walletBalanceRepository) {
        this.userRepository = userRepository;
        this.walletRepository = walletRepository;
        this.walletBalanceRepository = walletBalanceRepository;
    }

    @Transactional(readOnly = true)
    public Optional<Long> resolveReceiverWalletId(String receiverIdentifier) {
        if (receiverIdentifier == null || receiverIdentifier.isBlank()) return Optional.empty();
        String id = receiverIdentifier.trim();
        Optional<User> byUsername = userRepository.findByUsername(id);
        if (byUsername.isPresent()) {
            return walletRepository.findByUser_Id(byUsername.get().getId()).map(Wallet::getId);
        }
        Optional<Wallet> byWalletNumber = walletRepository.findByWalletNumber(id);
        if (byWalletNumber.isPresent()) {
            return Optional.of(byWalletNumber.get().getId());
        }
        Optional<User> byPhone = userRepository.findByPhone(id);
        if (byPhone.isPresent()) {
            return walletRepository.findByUser_Id(byPhone.get().getId()).map(Wallet::getId);
        }
        return Optional.empty();
    }

    @Transactional(readOnly = true)
    public boolean hasSufficientBalance(Long walletId, String currency, BigDecimal totalRequired) {
        return walletBalanceRepository.findByWalletIdAndCurrencyCode(walletId, currency)
                .map(b -> b.getAvailableBalance().compareTo(totalRequired) >= 0)
                .orElse(false);
    }
}
