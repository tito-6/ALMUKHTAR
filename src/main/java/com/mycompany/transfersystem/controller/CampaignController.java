package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.dto.campaign.CreateCampaignRequest;
import com.mycompany.transfersystem.entity.NotificationCampaign;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.campaign.CampaignService;
import com.mycompany.transfersystem.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/campaigns")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<NotificationCampaign> create(@Valid @RequestBody CreateCampaignRequest dto,
                                                         @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(campaignService.createCampaign(dto, user.getId()));
    }

    @PostMapping("/{id}/schedule")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<Void> schedule(@PathVariable Long id, @RequestParam String scheduledAt) {
        campaignService.scheduleCampaign(id, LocalDateTime.parse(scheduledAt));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/execute")
    @PreAuthorize("hasAnyRole('PLATFORM_OWNER','MOTHER_BRANCH_ADMIN')")
    public ResponseEntity<Void> execute(@PathVariable Long id) {
        campaignService.executeCampaign(id);
        return ResponseEntity.ok().build();
    }
}
