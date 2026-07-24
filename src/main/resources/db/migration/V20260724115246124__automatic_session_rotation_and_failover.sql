-- V20260724115246124__automatic_session_rotation_and_failover.sql
-- Description: Schema changes for automatic session rotation and failover.
-- JTBD: When implementing failover logic for this epic, I want to catch limits and errors to dynamically switch accounts, so that campaigns run continuously.

ALTER TABLE tg_accounts ADD COLUMN campaign_id VARCHAR(36);
ALTER TABLE tg_accounts ADD COLUMN daily_dispatch_count INT NOT NULL DEFAULT 0;
ALTER TABLE tg_accounts ADD COLUMN daily_dispatch_limit INT NOT NULL DEFAULT 50;

ALTER TABLE tg_accounts ADD CONSTRAINT fk_tg_accounts_campaign FOREIGN KEY (campaign_id) REFERENCES campaigns (id) ON DELETE SET NULL;
