package com.eneik.generated.leadgen.repository;

import com.eneik.generated.leadgen.model.ConversationMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, String> {
    List<ConversationMessage> findByConversationId(String conversationId, Pageable pageable);
    List<ConversationMessage> findByConversationIdAndIdLessThan(String conversationId, String id, Pageable pageable);
    long countByConversationIdAndSenderType(String conversationId, String senderType);
}
