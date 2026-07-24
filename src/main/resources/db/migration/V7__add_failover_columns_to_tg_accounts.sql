-- Down migration / rollback script:
-- DROP INDEX IF EXISTS idx_tg_accounts_campaign;
-- ALTER TABLE tg_accounts DROP COLUMN IF EXISTS campaign_id;
-- ALTER TABLE tg_accounts DROP COLUMN IF EXISTS daily_dispatch_limit;
-- ALTER TABLE tg_accounts DROP COLUMN IF EXISTS daily_dispatch_count;

-- Up Migration
ALTER TABLE tg_accounts ADD COLUMN campaign_id VARCHAR(36);
ALTER TABLE tg_accounts ADD COLUMN daily_dispatch_limit INT NOT NULL DEFAULT 50;
ALTER TABLE tg_accounts ADD COLUMN daily_dispatch_count INT NOT NULL DEFAULT 0;

CREATE INDEX idx_tg_accounts_campaign ON tg_accounts(campaign_id);
