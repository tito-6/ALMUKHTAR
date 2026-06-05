package com.mycompany.transfersystem.service.campaign;

import com.mycompany.transfersystem.dto.campaign.CreateCampaignRequest;
import com.mycompany.transfersystem.entity.InAppNotification;
import com.mycompany.transfersystem.entity.NotificationCampaign;
import com.mycompany.transfersystem.exception.ResourceNotFoundException;
import com.mycompany.transfersystem.repository.InAppNotificationRepository;
import com.mycompany.transfersystem.repository.NotificationCampaignRepository;
import com.mycompany.transfersystem.repository.UserRepository;
import com.mycompany.transfersystem.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class CampaignService {

    private final NotificationCampaignRepository campaignRepository;
    private final InAppNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    @Autowired(required = false)
    private AIMessageGeneratorService aiMessageGeneratorService;

    @Transactional
    public NotificationCampaign createCampaign(CreateCampaignRequest dto, Long creatorId) {
        NotificationCampaign campaign = NotificationCampaign.builder()
                .name(dto.getName())
                .targetAudience(NotificationCampaign.TargetAudience.valueOf(dto.getTargetAudience()))
                .messageTemplate(dto.getMessageTemplate())
                .createdBy(creatorId)
                .build();
        campaign = campaignRepository.save(campaign);
        var creator = userRepository.findById(creatorId).orElse(null);
        auditService.log("CAMPAIGN_CREATED", "CAMPAIGN", campaign.getId(), null, creator);
        return campaign;
    }

    @Transactional
    public void scheduleCampaign(Long campaignId, LocalDateTime scheduledAt) {
        NotificationCampaign c = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));
        c.setScheduledAt(scheduledAt);
        c.setStatus(NotificationCampaign.CampaignStatus.SCHEDULED);
        campaignRepository.save(c);
    }

    @Transactional
    public void executeCampaign(Long campaignId) {
        NotificationCampaign c = campaignRepository.findById(campaignId)
                .orElseThrow(() -> new ResourceNotFoundException("Campaign not found: " + campaignId));
        var users = userRepository.findAll();
        int count = 0;
        for (var user : users) {
            try {
                String message = c.getMessageTemplate();
                notificationRepository.save(InAppNotification.builder()
                        .userId(user.getId())
                        .title(c.getName())
                        .body(message)
                        .type(InAppNotification.NotificationType.PROMO)
                        .build());
                count++;
            } catch (Exception e) {
                log.warn("Failed to create notification for user {}: {}", user.getId(), e.getMessage());
            }
        }
        c.setSentCount(count);
        c.setStatus(NotificationCampaign.CampaignStatus.SENT);
        campaignRepository.save(c);
        log.info("Campaign {} executed: {} notifications sent", campaignId, count);
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        InAppNotification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        n.setDeliveryStatus(InAppNotification.DeliveryStatus.READ);
        notificationRepository.save(n);
    }
}
