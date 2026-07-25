package com.eneik.generated.config;

public final class CacheConstants {
    private CacheConstants() {}

    public static final String CACHE_CONVERSATIONS = "conversations";
    public static final String CACHE_MESSAGES = "messages";
    public static final String CACHE_CAMPAIGNS = "campaigns";
    public static final String CACHE_CAMPAIGN_BY_ID = "campaignById";

    public static final String KEY_CAMPAIGNS_ALL = "'all'";

    // Centralized Cache TTL configurations (in seconds)
    public static final long TTL_CONVERSATIONS_SEC = 10L;
    public static final long TTL_MESSAGES_SEC = 300L;
    public static final long TTL_CAMPAIGNS_SEC = 3600L;

    /**
     * Canonical key builder for conversations cache.
     * Enforces type-safety, anti-homonymy, and semantic consistency as a central registry.
     */
    public static String buildConversationsKey(String status, String assignedAgentId, int page, int limit) {
        String cleanStatus = status != null ? status.toUpperCase() : "ALL";
        String cleanAgent = assignedAgentId != null ? assignedAgentId : "";
        return String.format("%s_%s_%d_%d", cleanStatus, cleanAgent, page, limit);
    }
}
