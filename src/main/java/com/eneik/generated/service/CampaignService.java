package com.eneik.generated.service;

import com.eneik.generated.domain.Campaign;
import com.eneik.generated.repository.CampaignRepository;
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
    public Campaign saveCampaign(Campaign campaign) {
        return campaignRepository.save(campaign);
    }

    @Transactional(readOnly = true)
    public Optional<Campaign> getCampaign(String id) {
        return campaignRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public java.util.List<Campaign> getAllCampaigns() {
        return campaignRepository.findAll();
    }

    @Transactional(readOnly = true)
    public boolean existsById(String id) {
        return campaignRepository.existsById(id);
    }
}
