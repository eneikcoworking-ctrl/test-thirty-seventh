-- Down migration / rollback script
DROP INDEX IF EXISTS idx_tg_accounts_campaign;
ALTER TABLE tg_accounts DROP COLUMN IF EXISTS campaign_id;
ALTER TABLE tg_accounts DROP COLUMN IF EXISTS daily_dispatch_limit;
ALTER TABLE tg_accounts DROP COLUMN IF EXISTS daily_dispatch_count;
