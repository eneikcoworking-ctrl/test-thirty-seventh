CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO users (username, password_hash) VALUES ('admin', '$2a$10$gR9bIs8TjI.hYm5PMyu8eeZ/Uf0R6rA1vS3aA91Z7HmWxX4g7K/7y');
