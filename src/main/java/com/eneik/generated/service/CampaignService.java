package com.eneik.generated.service;

import com.eneik.generated.domain.Campaign;
import com.eneik.generated.repository.CampaignRepository;
import com.eneik.generated.config.CacheConstants;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Optional;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;

    public CampaignService(CampaignRepository campaignRepository) {
        this.campaignRepository = campaignRepository;
    }

    @Transactional
    @CacheEvict(value = {CacheConstants.CACHE_CAMPAIGNS, CacheConstants.CACHE_CAMPAIGN_BY_ID}, allEntries = true)
    public Campaign saveCampaign(Campaign campaign) {
        return campaignRepository.save(campaign);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.CACHE_CAMPAIGN_BY_ID, key = "#id")
    public Optional<Campaign> getCampaign(String id) {
        return campaignRepository.findById(id);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = CacheConstants.CACHE_CAMPAIGNS, key = CacheConstants.KEY_CAMPAIGNS_ALL)
    public List<Campaign> getAllCampaigns() {
        return campaignRepository.findAll();
    }
}
