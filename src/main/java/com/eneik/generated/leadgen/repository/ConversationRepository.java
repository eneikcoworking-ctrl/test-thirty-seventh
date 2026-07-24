package com.eneik.generated.leadgen.repository;

import com.eneik.generated.leadgen.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Optional<Conversation> findByTelegramAccountIdAndLeadId(String telegramAccountId, String leadId);
}
