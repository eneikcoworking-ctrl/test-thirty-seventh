-- Up Migration
ALTER TABLE tg_accounts ADD COLUMN daily_sent_count INT NOT NULL DEFAULT 0;
ALTER TABLE tg_accounts ADD COLUMN daily_limit INT NOT NULL DEFAULT 20;

-- Down Migration
-- ALTER TABLE tg_accounts DROP COLUMN daily_sent_count;
-- ALTER TABLE tg_accounts DROP COLUMN daily_limit;
