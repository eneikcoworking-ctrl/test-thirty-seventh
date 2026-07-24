-- Down migration / rollback script
DROP INDEX IF EXISTS idx_outbound_dispatches_account_time;
DROP TABLE IF EXISTS outbound_dispatches;
