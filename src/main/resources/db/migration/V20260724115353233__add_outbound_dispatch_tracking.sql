-- Down migration / rollback script:
-- DROP INDEX IF EXISTS idx_outbound_dispatches_account_time;
-- DROP TABLE IF EXISTS outbound_dispatches;

-- Up Migration
CREATE TABLE outbound_dispatches (
    id BIGSERIAL PRIMARY KEY,
    tg_account_id BIGINT NOT NULL,
    campaign_id VARCHAR(36) NOT NULL,
    recipient_phone_or_username VARCHAR(255) NOT NULL,
    dispatched_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_outbound_dispatches_tg_account FOREIGN KEY (tg_account_id) REFERENCES tg_accounts (id) ON DELETE CASCADE,
    CONSTRAINT fk_outbound_dispatches_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns (id) ON DELETE CASCADE
);

CREATE INDEX idx_outbound_dispatches_account_time ON outbound_dispatches (tg_account_id, dispatched_at);
