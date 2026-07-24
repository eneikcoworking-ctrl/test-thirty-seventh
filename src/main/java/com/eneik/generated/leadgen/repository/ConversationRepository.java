package com.eneik.generated.leadgen.repository;

import com.eneik.generated.leadgen.model.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {
    Page<Conversation> findByStatus(String status, Pageable pageable);
    Page<Conversation> findByAssignedAgentId(String assignedAgentId, Pageable pageable);
    Page<Conversation> findByStatusAndAssignedAgentId(String status, String assignedAgentId, Pageable pageable);
}
