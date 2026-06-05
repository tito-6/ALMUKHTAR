package com.mycompany.transfersystem.service.trading;

import com.mycompany.transfersystem.entity.TradingRiskProfile;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.entity.Wallet;
import com.mycompany.transfersystem.entity.enums.AssetClass;
import com.mycompany.transfersystem.entity.enums.KycTier;
import com.mycompany.transfersystem.entity.enums.TradingRiskArchetype;
import com.mycompany.transfersystem.repository.TradingRiskProfileRepository;
import com.mycompany.transfersystem.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.EnumSet;

@Service
public class TradingRiskProfileService {

    private final TradingRiskProfileRepository tradingRiskProfileRepository;
    private final WalletRepository walletRepository;

    public TradingRiskProfileService(TradingRiskProfileRepository tradingRiskProfileRepository,
                                     WalletRepository walletRepository) {
        this.tradingRiskProfileRepository = tradingRiskProfileRepository;
        this.walletRepository = walletRepository;
    }

    @Transactional(readOnly = true)
    public TradingRiskProfile getOrCreateDefault(User user) {
        return tradingRiskProfileRepository.findByUser_Id(user.getId())
                .orElseGet(() -> tradingRiskProfileRepository.save(buildDefaults(user, TradingRiskArchetype.CONSERVATIVE)));
    }

    @Transactional
    public TradingRiskProfile updateArchetype(User user, TradingRiskArchetype archetype) {
        Wallet w = findWallet(user);
        if (!walletMeetsArchetype(w, archetype)) {
            throw new com.mycompany.transfersystem.exception.ConditionNotMetException(
                    "Current KYC tier does not meet requirements for risk profile " + archetype);
        }
        TradingRiskProfile existing = tradingRiskProfileRepository.findByUser_Id(user.getId()).orElse(null);
        if (existing == null) {
            return tradingRiskProfileRepository.save(buildDefaults(user, archetype));
        }
        applyArchetype(existing, archetype);
        return tradingRiskProfileRepository.save(existing);
    }

    public KycTier minimumKycFor(TradingRiskArchetype archetype) {
        return switch (archetype) {
            case CONSERVATIVE -> KycTier.BASIC;
            case BALANCED -> KycTier.STANDARD;
            case AGGRESSIVE, PROFESSIONAL -> KycTier.PREMIUM;
        };
    }

    public boolean walletMeetsArchetype(Wallet wallet, TradingRiskArchetype archetype) {
        KycTier required = minimumKycFor(archetype);
        KycTier actual = wallet != null ? wallet.getKycTier() : KycTier.BASIC;
        return actual.ordinal() >= required.ordinal();
    }

    public Wallet findWallet(User user) {
        return walletRepository.findByUser_Id(user.getId()).orElse(null);
    }

    public void assertKycAllowsProfile(User user, TradingRiskProfile profile) {
        Wallet w = findWallet(user);
        if (!walletMeetsArchetype(w, profile.getArchetype())) {
            throw new com.mycompany.transfersystem.exception.ConditionNotMetException(
                    "Current KYC tier does not meet requirements for risk profile " + profile.getArchetype());
        }
    }

    public void assertAssetClassAllowed(TradingRiskProfile profile, AssetClass assetClass) {
        if (profile.getAllowedAssetClasses() == null || profile.getAllowedAssetClasses().isEmpty()) {
            throw new com.mycompany.transfersystem.exception.ConditionNotMetException(
                    "Risk profile has no permitted asset classes");
        }
        if (!profile.getAllowedAssetClasses().contains(assetClass)) {
            throw new com.mycompany.transfersystem.exception.ConditionNotMetException(
                    "Asset class " + assetClass + " is not permitted for your trading risk profile");
        }
    }

    private TradingRiskProfile buildDefaults(User user, TradingRiskArchetype archetype) {
        TradingRiskProfile p = TradingRiskProfile.builder()
                .user(user)
                .archetype(archetype)
                .maxSingleOrderUsd(BigDecimal.ZERO)
                .dailyTradingCapUsd(BigDecimal.ZERO)
                .allowedAssetClasses(EnumSet.noneOf(AssetClass.class))
                .build();
        applyArchetype(p, archetype);
        return p;
    }

    private void applyArchetype(TradingRiskProfile p, TradingRiskArchetype archetype) {
        p.setArchetype(archetype);
        switch (archetype) {
            case CONSERVATIVE:
                p.setMaxSingleOrderUsd(new BigDecimal("5000"));
                p.setDailyTradingCapUsd(new BigDecimal("10000"));
                p.setAllowedAssetClasses(EnumSet.of(AssetClass.STOCK, AssetClass.ETF));
                break;
            case BALANCED:
                p.setMaxSingleOrderUsd(new BigDecimal("15000"));
                p.setDailyTradingCapUsd(new BigDecimal("50000"));
                p.setAllowedAssetClasses(EnumSet.of(AssetClass.STOCK, AssetClass.ETF, AssetClass.FOREX));
                break;
            case AGGRESSIVE:
                p.setMaxSingleOrderUsd(new BigDecimal("50000"));
                p.setDailyTradingCapUsd(new BigDecimal("250000"));
                p.setAllowedAssetClasses(EnumSet.allOf(AssetClass.class));
                break;
            case PROFESSIONAL:
                p.setMaxSingleOrderUsd(new BigDecimal("500000"));
                p.setDailyTradingCapUsd(new BigDecimal("5000000"));
                p.setAllowedAssetClasses(EnumSet.allOf(AssetClass.class));
                break;
            default:
                break;
        }
    }
}
