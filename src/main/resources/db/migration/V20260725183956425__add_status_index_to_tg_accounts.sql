-- V20260725183956425__add_status_index_to_tg_accounts.sql
-- Optimizes background session health worker queries by indexing the status column.
CREATE INDEX idx_tg_accounts_status ON tg_accounts(status);
