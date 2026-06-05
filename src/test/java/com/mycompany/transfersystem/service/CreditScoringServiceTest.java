package com.mycompany.transfersystem.service;

import com.mycompany.transfersystem.entity.CreditProfile;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.Wallet;
import com.mycompany.transfersystem.entity.enums.KycTier;
import com.mycompany.transfersystem.repository.*;
import com.mycompany.transfersystem.service.lending.CreditScoringService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CreditScoringServiceTest {

    @Mock private CreditProfileRepository creditProfileRepository;
    @Mock private TransactionRepository transactionRepository;
    @Mock private TrustScoreRepository trustScoreRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private LoanRepaymentScheduleRepository scheduleRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private CreditScoringService creditScoringService;

    @Test
    void calculateScore_allSixComponentsCorrect() {
        Long userId = 1L;
        User user = new User(); user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(creditProfileRepository.findByUser_Id(userId)).thenReturn(Optional.of(CreditProfile.builder().user(user).build()));
        when(transactionRepository.sumAmountByUserSince(eq(userId), any())).thenReturn(new BigDecimal("10000"));
        when(transactionRepository.countCompletedByUserSince(eq(userId), any())).thenReturn(20L);
        when(trustScoreRepository.findByUser_Id(userId)).thenReturn(Optional.of(
                com.mycompany.transfersystem.entity.TrustScore.builder().user(user).score(80).build()));
        when(scheduleRepository.findSchedulesByUserId(userId)).thenReturn(Collections.emptyList());
        Wallet wallet = Wallet.builder().kycTier(KycTier.PREMIUM).createdAt(Instant.now().minus(365, ChronoUnit.DAYS)).build();
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.of(wallet));
        when(creditProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CreditProfile result = creditScoringService.calculateAndSave(userId);
        assertTrue(result.getCreditScore() > 0);
        assertNotNull(result.getScoreComponents());
    }

    @Test
    void calculateScore_newUser_neutralRepaymentScore() {
        Long userId = 2L;
        User user = new User(); user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(creditProfileRepository.findByUser_Id(userId)).thenReturn(Optional.empty());
        when(transactionRepository.sumAmountByUserSince(eq(userId), any())).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.countCompletedByUserSince(eq(userId), any())).thenReturn(0L);
        when(trustScoreRepository.findByUser_Id(userId)).thenReturn(Optional.empty());
        when(scheduleRepository.findSchedulesByUserId(userId)).thenReturn(Collections.emptyList());
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.empty());
        when(creditProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CreditProfile result = creditScoringService.calculateAndSave(userId);
        assertEquals(100 + 20, result.getCreditScore()); // neutral repayment 100 + KYC_BASIC 20
    }

    @Test
    void calculateScore_missedPayments_reducesScore() {
        Long userId = 3L;
        User user = new User(); user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(creditProfileRepository.findByUser_Id(userId)).thenReturn(Optional.of(CreditProfile.builder().user(user).build()));
        when(transactionRepository.sumAmountByUserSince(eq(userId), any())).thenReturn(new BigDecimal("5000"));
        when(transactionRepository.countCompletedByUserSince(eq(userId), any())).thenReturn(10L);
        when(trustScoreRepository.findByUser_Id(userId)).thenReturn(Optional.empty());
        var missed1 = com.mycompany.transfersystem.entity.LoanRepaymentSchedule.builder().status("MISSED").build();
        var missed2 = com.mycompany.transfersystem.entity.LoanRepaymentSchedule.builder().status("MISSED").build();
        var missed3 = com.mycompany.transfersystem.entity.LoanRepaymentSchedule.builder().status("MISSED").build();
        when(scheduleRepository.findSchedulesByUserId(userId)).thenReturn(List.of(missed1, missed2, missed3));
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.empty());
        when(creditProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CreditProfile result = creditScoringService.calculateAndSave(userId);
        assertTrue(result.getCreditScore() < 500);
    }

    @Test
    void calculateScore_ineligibleTier_lowScore() {
        Long userId = 4L;
        User user = new User(); user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(creditProfileRepository.findByUser_Id(userId)).thenReturn(Optional.of(CreditProfile.builder().user(user).build()));
        when(transactionRepository.sumAmountByUserSince(eq(userId), any())).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.countCompletedByUserSince(eq(userId), any())).thenReturn(0L);
        when(trustScoreRepository.findByUser_Id(userId)).thenReturn(Optional.empty());
        when(scheduleRepository.findSchedulesByUserId(userId)).thenReturn(Collections.emptyList());
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.empty());
        when(creditProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CreditProfile result = creditScoringService.calculateAndSave(userId);
        assertEquals("INELIGIBLE", result.getRiskTier());
    }

    @Test
    void calculateScore_tierA_highVolume() {
        Long userId = 5L;
        User user = new User(); user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(creditProfileRepository.findByUser_Id(userId)).thenReturn(Optional.of(CreditProfile.builder().user(user).build()));
        when(transactionRepository.sumAmountByUserSince(eq(userId), any())).thenReturn(new BigDecimal("100000"));
        when(transactionRepository.countCompletedByUserSince(eq(userId), any())).thenReturn(50L);
        when(trustScoreRepository.findByUser_Id(userId)).thenReturn(Optional.of(
                com.mycompany.transfersystem.entity.TrustScore.builder().user(user).score(100).build()));
        var paid = com.mycompany.transfersystem.entity.LoanRepaymentSchedule.builder().status("PAID").build();
        when(scheduleRepository.findSchedulesByUserId(userId)).thenReturn(List.of(paid, paid, paid));
        Wallet wallet = Wallet.builder().kycTier(KycTier.PREMIUM).createdAt(Instant.now().minus(730, ChronoUnit.DAYS)).build();
        when(walletRepository.findByUser_Id(userId)).thenReturn(Optional.of(wallet));
        when(creditProfileRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        CreditProfile result = creditScoringService.calculateAndSave(userId);
        assertEquals("TIER_A", result.getRiskTier());
    }
}
