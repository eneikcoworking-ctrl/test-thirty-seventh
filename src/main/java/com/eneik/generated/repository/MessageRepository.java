package com.eneik.generated.repository;

import com.eneik.generated.model.Message;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByDialogIdOrderByReceivedAtDesc(Long dialogId, Pageable pageable);
    long countByDialogId(Long dialogId);
}
