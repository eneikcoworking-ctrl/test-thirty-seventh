CREATE TABLE outbound_dispatches (
    id BIGSERIAL PRIMARY KEY,
    tg_account_id BIGINT NOT NULL,
    campaign_id VARCHAR(36),
    dispatched_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_outbound_dispatch_account FOREIGN KEY (tg_account_id) REFERENCES tg_accounts(id) ON DELETE CASCADE
);

CREATE INDEX idx_outbound_dispatches_account_time ON outbound_dispatches(tg_account_id, dispatched_at);
