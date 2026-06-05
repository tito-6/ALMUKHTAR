package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.entity.InAppNotification;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.InAppNotificationRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.campaign.CampaignService;
import com.mycompany.transfersystem.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class NotificationController {

    private final InAppNotificationRepository notificationRepository;
    private final CampaignService campaignService;
    private final UserRepository userRepository;

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<InAppNotification>> my(@AuthenticationPrincipal UserDetails userDetails, Pageable pageable) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId(), pageable));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markRead(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        campaignService.markRead(id, user.getId());
        return ResponseEntity.ok().build();
    }
}
