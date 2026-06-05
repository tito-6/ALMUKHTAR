package com.mycompany.transfersystem.service.campaign;

import com.mycompany.transfersystem.service.ai.AlmukhtarAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(name = "almukhtar.ai.enabled", havingValue = "true", matchIfMissing = false)
public class AIMessageGeneratorService {

    private final AlmukhtarAiService aiService;

    @Async("notificationExecutor")
    public CompletableFuture<String> generatePersonalizedMessage(Long userId, String templateHint) {
        try {
            String prompt = "Generate a personalized notification for user " + userId + " based on: " + templateHint;
            String result = aiService.chat(prompt);
            return CompletableFuture.completedFuture(result);
        } catch (Exception e) {
            log.warn("AI message generation failed for user {}: {}", userId, e.getMessage());
            return CompletableFuture.completedFuture(templateHint);
        }
    }
}
