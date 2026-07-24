-- Down migration / rollback script:
-- DROP TABLE IF EXISTS leads;
-- DROP TABLE IF EXISTS campaigns;

CREATE TABLE campaigns (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    spintax_rules TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE leads (
    id VARCHAR(36) PRIMARY KEY,
    campaign_id VARCHAR(36) NOT NULL,
    username VARCHAR(255),
    phone_number VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'NEW',
    metadata TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_leads_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns (id) ON DELETE CASCADE
);
