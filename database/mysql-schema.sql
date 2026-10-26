-- Run in a NEW database dedicated to this project. No DROP/DELETE or existing-data migration.
CREATE DATABASE IF NOT EXISTS pulse_fitness CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE pulse_fitness;

CREATE TABLE IF NOT EXISTS pulse_users (
    id VARCHAR(36) PRIMARY KEY,
    phone VARCHAR(11) NOT NULL UNIQUE,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    created_at BIGINT NOT NULL,
    last_login_at BIGINT NOT NULL
);

CREATE TABLE IF NOT EXISTS pulse_login_events (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    logged_in_at BIGINT NOT NULL,
    login_method VARCHAR(24) NOT NULL,
    CONSTRAINT fk_pulse_login_user FOREIGN KEY (user_id) REFERENCES pulse_users(id)
);

-- Create a dedicated account separately in your database tool. Do not put your root password here.
-- Grant this account SELECT, INSERT, UPDATE on pulse_fitness.*.
