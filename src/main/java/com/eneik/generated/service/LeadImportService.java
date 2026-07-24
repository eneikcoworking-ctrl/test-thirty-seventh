package com.eneik.generated.service;

import com.eneik.generated.domain.Lead;
import com.eneik.generated.repository.LeadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class LeadImportService {

    private final LeadRepository leadRepository;

    public LeadImportService(LeadRepository leadRepository) {
        this.leadRepository = leadRepository;
    }

    /**
     * Imports leads from a raw string representing CSV or plain text lines.
     * Each line can represent:
     * - A CSV line: "username,phoneNumber,metadata" or "username,phoneNumber"
     * - A simple string representing either username (starts with '@') or a phoneNumber.
     *
     * @param campaignId the ID of the campaign to associate leads with
     * @param content raw string content of the lead import
     * @return list of imported Lead entities
     */
    @Transactional
    public List<Lead> importLeads(String campaignId, String content) {
        List<Lead> importedLeads = new ArrayList<>();
        if (content == null || content.trim().isEmpty()) {
            return importedLeads;
        }

        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue; // skip empty lines or comments
                }

                String username = null;
                String phoneNumber = null;
                String metadata = null;

                // Simple check for CSV vs single entry
                if (line.contains(",")) {
                    String[] parts = line.split(",", -1);
                    if (parts.length > 0) {
                        username = parts[0].trim();
                        if (username.isEmpty()) username = null;
                    }
                    if (parts.length > 1) {
                        phoneNumber = parts[1].trim();
                        if (phoneNumber.isEmpty()) phoneNumber = null;
                    }
                    if (parts.length > 2) {
                        // Gather remaining parts as metadata if there are commas in metadata
                        StringBuilder metadataBuilder = new StringBuilder();
                        for (int i = 2; i < parts.length; i++) {
                            if (i > 2) metadataBuilder.append(",");
                            metadataBuilder.append(parts[i]);
                        }
                        metadata = metadataBuilder.toString().trim();
                        if (metadata.isEmpty()) metadata = null;
                    }
                } else {
                    // Single entry
                    if (line.startsWith("@")) {
                        username = line;
                    } else if (line.matches("\\+?\\d+")) {
                        phoneNumber = line;
                    } else {
                        username = line;
                    }
                }

                Lead lead = new Lead(
                        UUID.randomUUID().toString(),
                        campaignId,
                        username,
                        phoneNumber,
                        "NEW",
                        metadata
                );
                importedLeads.add(leadRepository.save(lead));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse lead import", e);
        }

        return importedLeads;
    }
}
