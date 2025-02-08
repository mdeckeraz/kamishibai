-- Account table
CREATE TABLE IF NOT EXISTS accounts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL
);

-- Board table
CREATE TABLE IF NOT EXISTS boards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    owner_id BIGINT NOT NULL,
    FOREIGN KEY (owner_id) REFERENCES accounts(id)
);

-- Board Shared Users table
CREATE TABLE IF NOT EXISTS board_shared_users (
    board_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (board_id, user_id),
    FOREIGN KEY (board_id) REFERENCES boards(id),
    FOREIGN KEY (user_id) REFERENCES accounts(id)
);

-- Card table
CREATE TABLE IF NOT EXISTS cards (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    board_id BIGINT NOT NULL,
    FOREIGN KEY (board_id) REFERENCES boards(id)
);
