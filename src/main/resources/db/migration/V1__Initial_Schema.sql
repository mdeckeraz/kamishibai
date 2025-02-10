-- Initial schema migration
-- This is a placeholder migration since we're using baseline-on-migrate=true
-- The actual schema is already in place

-- Create accounts table
CREATE TABLE accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create boards table
CREATE TABLE boards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (owner_id) REFERENCES accounts(id)
);

-- Create board_shared_users table
CREATE TABLE board_shared_users (
    board_id BIGINT NOT NULL,
    account_id BIGINT NOT NULL,
    PRIMARY KEY (board_id, account_id),
    FOREIGN KEY (board_id) REFERENCES boards(id),
    FOREIGN KEY (account_id) REFERENCES accounts(id)
);

-- Create cards table
CREATE TABLE cards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    details TEXT,
    position INT NOT NULL,
    state VARCHAR(20) NOT NULL DEFAULT 'RED',
    reset_time TIME NOT NULL,
    image_url VARCHAR(255),
    board_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (board_id) REFERENCES boards(id)
);

-- Create card_audit_log table
CREATE TABLE card_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    card_id BIGINT NOT NULL,
    previous_state VARCHAR(20) NOT NULL,
    new_state VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (card_id) REFERENCES cards(id)
);
