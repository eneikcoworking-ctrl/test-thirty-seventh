package com.eneik.generated.repository;

import com.eneik.generated.domain.TgAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TgAccountRepository extends JpaRepository<TgAccount, Long> {
    Optional<TgAccount> findByPhoneNumber(String phoneNumber);

    List<TgAccount> findByCampaignIdAndStatusIgnoreCaseOrderByIdAsc(String campaignId, String status);

    @Query("SELECT a FROM TgAccount a WHERE a.campaignId = :campaignId AND LOWER(a.status) != LOWER(:statusExcluding) AND a.dailyDispatchCount < a.dailyDispatchLimit ORDER BY a.id ASC")
    List<TgAccount> findEligibleAccounts(@Param("campaignId") String campaignId, @Param("statusExcluding") String statusExcluding);

    @Modifying
    @Query("UPDATE TgAccount t SET t.status = :newStatus, t.updatedAt = :updatedAt WHERE t.id = :id AND t.status = :expectedOldStatus")
    int updateStatusGuarded(@Param("id") Long id, @Param("newStatus") String newStatus, @Param("expectedOldStatus") String expectedOldStatus, @Param("updatedAt") LocalDateTime updatedAt);
}
