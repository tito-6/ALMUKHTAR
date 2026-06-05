package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.entity.ReferralCode;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.ReferralCodeRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/referral")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ReferralController {

    private final ReferralCodeRepository referralCodeRepository;
    private final UserRepository userRepository;

    @GetMapping("/my-code")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Object>> myCode(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        ReferralCode rc = referralCodeRepository.findByUser_Id(user.getId())
                .orElseGet(() -> {
                    String code = UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
                    ReferralCode newRc = ReferralCode.builder()
                            .user(user)
                            .code(code)
                            .totalReferrals(0)
                            .totalRewardsGiven(BigDecimal.ZERO)
                            .build();
                    return referralCodeRepository.save(newRc);
                });
        return ResponseEntity.ok(Map.of(
                "referralCode", rc.getCode(),
                "totalReferrals", rc.getTotalReferrals(),
                "rewardsEarned", rc.getTotalRewardsGiven(),
                "currency", "USD"
        ));
    }
}
