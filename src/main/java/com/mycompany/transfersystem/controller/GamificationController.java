package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.BadgeResponse;
import com.mycompany.transfersystem.service.GamificationService;
import com.mycompany.transfersystem.dto.TrustScoreResponse;
import com.mycompany.transfersystem.entity.TrustScore;
import com.mycompany.transfersystem.repository.TrustScoreRepository;
import com.mycompany.transfersystem.repository.BadgeRepository;
import com.mycompany.transfersystem.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gamification")
@CrossOrigin(origins = "*")
public class GamificationController {

    private final GamificationService gamificationService;
    private final TrustScoreRepository trustScoreRepository;
    private final BadgeRepository badgeRepository;
    private final com.mycompany.transfersystem.repository.UserRepository userRepository;

    public GamificationController(GamificationService gamificationService,
                                  TrustScoreRepository trustScoreRepository,
                                  BadgeRepository badgeRepository,
                                  com.mycompany.transfersystem.repository.UserRepository userRepository) {
        this.gamificationService = gamificationService;
        this.trustScoreRepository = trustScoreRepository;
        this.badgeRepository = badgeRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/my-score")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TrustScoreResponse> myScore(@AuthenticationPrincipal UserDetails ud) {
        Long userId = SecurityUtils.resolveUserId(ud, userRepository);
        TrustScore ts = trustScoreRepository.findByUser_Id(userId)
                .orElse(TrustScore.builder().score(0).tier("BRONZE").feeDiscountPct(java.math.BigDecimal.ZERO).build());
        return ResponseEntity.ok(TrustScoreResponse.from(ts));
    }

    @GetMapping("/my-badges")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BadgeResponse>> myBadges(@AuthenticationPrincipal UserDetails ud) {
        Long userId = SecurityUtils.resolveUserId(ud, userRepository);
        return ResponseEntity.ok(badgeRepository.findByUser_Id(userId).stream()
                .map(BadgeResponse::from)
                .collect(Collectors.toList()));
    }

    @GetMapping("/leaderboard")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','SUPER_ADMIN','BRANCH_MANAGER')")
    public ResponseEntity<Page<TrustScoreResponse>> leaderboard(Pageable pageable) {
        return ResponseEntity.ok(trustScoreRepository.findAllByOrderByScoreDesc(pageable)
                .map(TrustScoreResponse::from));
    }
}
