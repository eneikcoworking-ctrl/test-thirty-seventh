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

    List<TgAccount> findByCampaignIdAndStatusIgnoreCaseOrderByIdAsc(String campaignId, String status);

    @Query("SELECT a FROM TgAccount a WHERE a.campaignId = :campaignId AND LOWER(a.status) != LOWER(:statusExcluding) AND a.dailyDispatchCount < a.dailyDispatchLimit ORDER BY a.id ASC")
    List<TgAccount> findEligibleAccounts(@Param("campaignId") String campaignId, @Param("statusExcluding") String statusExcluding);
}
