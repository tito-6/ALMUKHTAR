package com.mycompany.transfersystem.controller;

import com.mycompany.transfersystem.entity.InAppNotification;
import com.mycompany.transfersystem.entity.User;
import com.mycompany.transfersystem.repository.InAppNotificationRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.util.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final InAppNotificationRepository notificationRepository;
    private final UserRepository userRepository;

    public NotificationController(InAppNotificationRepository notificationRepository, UserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<InAppNotification>> getMyNotifications(
            @AuthenticationPrincipal UserDetails userDetails,
            Pageable pageable) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        return ResponseEntity.ok(notificationRepository.findByUser_IdOrderByCreatedAtDesc(user.getId(), pageable));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, Long>> getUnreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        long count = notificationRepository.countByUser_IdAndIsReadFalse(user.getId());
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        User user = SecurityUtils.resolveUser(userDetails, userRepository);
        InAppNotification n = notificationRepository.findById(id)
                .orElseThrow(() -> new com.mycompany.transfersystem.exception.ResourceNotFoundException("Notification not found"));
        if (!n.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Not your notification");
        }
        n.setIsRead(true);
        notificationRepository.save(n);
        return ResponseEntity.noContent().build();
    }
}
