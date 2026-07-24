package com.eneik.generated.repository;

import com.eneik.generated.domain.TgAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface TgAccountRepository extends JpaRepository<TgAccount, Long> {
    Optional<TgAccount> findByPhoneNumber(String phoneNumber);

    @Query("SELECT t FROM TgAccount t WHERE t.campaignId = :campaignId AND t.status = :status AND t.dailyDispatchCount < t.dailyDispatchLimit ORDER BY t.id ASC")
    List<TgAccount> findEligibleAccounts(@Param("campaignId") String campaignId, @Param("status") String status);
}
