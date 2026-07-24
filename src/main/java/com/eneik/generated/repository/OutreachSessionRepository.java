package com.eneik.generated.repository;

import com.eneik.generated.domain.OutreachSession;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OutreachSessionRepository extends JpaRepository<OutreachSession, Long> {
    Optional<OutreachSession> findByTgAccountIdAndLeadIdentifier(Long tgAccountId, String leadIdentifier);
}
