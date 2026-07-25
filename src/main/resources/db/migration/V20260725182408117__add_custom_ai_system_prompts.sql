-- Down migration / rollback script:
-- ALTER TABLE campaigns DROP COLUMN IF EXISTS system_prompt;
-- ALTER TABLE campaigns DROP COLUMN IF EXISTS ai_persona;
-- ALTER TABLE campaigns DROP COLUMN IF EXISTS sales_goals;
-- ALTER TABLE campaigns DROP COLUMN IF EXISTS tone_of_voice;
-- ALTER TABLE campaigns DROP COLUMN IF EXISTS product_faqs;
-- ALTER TABLE campaigns DROP COLUMN IF EXISTS qualification_rules;

-- Up migration
ALTER TABLE campaigns ADD COLUMN system_prompt TEXT;
ALTER TABLE campaigns ADD COLUMN ai_persona TEXT;
ALTER TABLE campaigns ADD COLUMN sales_goals TEXT;
ALTER TABLE campaigns ADD COLUMN tone_of_voice TEXT;
ALTER TABLE campaigns ADD COLUMN product_faqs TEXT;
ALTER TABLE campaigns ADD COLUMN qualification_rules TEXT;
