package com.eneik.generated.repository;

import com.eneik.generated.domain.OutboundDispatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;

@Repository
public interface OutboundDispatchRepository extends JpaRepository<OutboundDispatch, Long> {
    long countByTgAccountIdAndDispatchedAtAfter(Long tgAccountId, LocalDateTime threshold);
}
