CREATE TABLE proxies (
    id BIGSERIAL PRIMARY KEY,
    ip_address VARCHAR(255) NOT NULL,
    port INT NOT NULL,
    protocol VARCHAR(50) NOT NULL,
    username VARCHAR(255),
    password VARCHAR(255)
);

CREATE TABLE tg_accounts (
    id BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(255) NOT NULL UNIQUE,
    session_data TEXT,
    status VARCHAR(50) NOT NULL,
    proxy_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_proxy FOREIGN KEY (proxy_id) REFERENCES proxies (id)
);
