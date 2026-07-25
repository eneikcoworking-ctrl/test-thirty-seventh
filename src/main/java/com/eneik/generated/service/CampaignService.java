package com.eneik.generated.service;

import com.eneik.generated.domain.Campaign;
import com.eneik.generated.repository.CampaignRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CachePut;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;

    public CampaignService(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    @Transactional
    @CachePut(value = "campaigns", key = "#result.id")
    public Campaign saveCampaign(Campaign campaign) {
        return campaignRepository.save(campaign);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "campaigns", key = "#id")
    public Optional<Campaign> getCampaign(String id) {
        return campaignRepository.findById(id);
    }
}
