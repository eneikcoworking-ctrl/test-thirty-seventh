package com.eneik.generated.repository;

import com.eneik.generated.domain.Lead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface LeadRepository extends JpaRepository<Lead, String> {
    List<Lead> findByCampaignId(String campaignId);
}
