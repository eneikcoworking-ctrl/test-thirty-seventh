ALTER TABLE tg_accounts ADD COLUMN daily_limit INT DEFAULT 20;

CREATE TABLE daily_limit_trackers (
    id BIGSERIAL PRIMARY KEY,
    tg_account_id BIGINT NOT NULL,
    tracked_date DATE NOT NULL,
    sent_count INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_daily_limit_tg_account FOREIGN KEY (tg_account_id) REFERENCES tg_accounts (id),
    CONSTRAINT uq_account_date UNIQUE (tg_account_id, tracked_date)
);

CREATE TABLE outreach_sessions (
    id BIGSERIAL PRIMARY KEY,
    tg_account_id BIGINT NOT NULL,
    lead_identifier VARCHAR(255) NOT NULL,
    message_count INT NOT NULL DEFAULT 0,
    is_blocked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_outreach_session_tg_account FOREIGN KEY (tg_account_id) REFERENCES tg_accounts (id),
    CONSTRAINT uq_account_lead UNIQUE (tg_account_id, lead_identifier)
);
