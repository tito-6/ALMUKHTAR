package com.mycompany.transfersystem.repository;

import com.mycompany.transfersystem.entity.AiConversationMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AiConversationMessageRepository extends JpaRepository<AiConversationMessage, Long> {

    List<AiConversationMessage> findBySession_IdOrderByCreatedAtAsc(Long sessionId);
}
