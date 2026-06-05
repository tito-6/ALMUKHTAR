package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.AiConversationSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiConversationSessionRepository extends JpaRepository<AiConversationSession, Long> {

    List<AiConversationSession> findByUser_IdOrderByCreatedAtDesc(Long userId);

    Optional<AiConversationSession> findByUser_IdAndClientSessionKey(Long userId, String clientSessionKey);

    Optional<AiConversationSession> findByIdAndUser_Id(Long id, Long userId);

    Optional<AiConversationSession> findByExternalConversationId(String externalConversationId);
}
