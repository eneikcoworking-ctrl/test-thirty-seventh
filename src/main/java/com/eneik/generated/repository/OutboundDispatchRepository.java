package com.eneik.generated.repository;

import com.eneik.generated.domain.OutboundDispatch;
import com.eneik.generated.domain.TgAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;

@Repository
public interface OutboundDispatchRepository extends JpaRepository<OutboundDispatch, Long> {

    long countByTgAccountAndDispatchedAtAfter(TgAccount tgAccount, OffsetDateTime timestamp);
}
