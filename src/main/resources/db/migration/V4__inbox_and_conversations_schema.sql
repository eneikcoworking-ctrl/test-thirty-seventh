CREATE TABLE conversations (
    id VARCHAR(36) PRIMARY KEY,
    telegram_chat_id BIGINT NOT NULL,
    lead_name VARCHAR(255),
    lead_username VARCHAR(255),
    lead_phone VARCHAR(255),
    status VARCHAR(50) NOT NULL,
    assigned_agent_id VARCHAR(36),
    last_message_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE conversation_messages (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    text VARCHAR(2000) NOT NULL,
    sender_type VARCHAR(50) NOT NULL,
    sent_at TIMESTAMP WITH TIME ZONE NOT NULL,
    sender_name VARCHAR(255),
    CONSTRAINT fk_conversation FOREIGN KEY (conversation_id) REFERENCES conversations(id) ON DELETE CASCADE
);
