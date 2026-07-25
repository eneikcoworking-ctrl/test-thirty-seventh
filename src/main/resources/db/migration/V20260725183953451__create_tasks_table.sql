-- Down migration / rollback script:
-- DROP TABLE IF EXISTS tasks;

-- Up Migration
CREATE TABLE tasks (
    id VARCHAR(36) PRIMARY KEY,
    status VARCHAR(50) NOT NULL,
    rejection_count INT NOT NULL DEFAULT 0,
    session_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
