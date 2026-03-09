-- ===========================
-- CliniQ Database Schema
-- Database: msc
-- Schema: cliniq
-- ===========================

-- Create schema if not exists
CREATE SCHEMA IF NOT EXISTS cliniq;

-- Set search path
SET search_path TO cliniq;

-- ===========================
-- Users Table (Staff accounts)
-- ===========================
CREATE TABLE IF NOT EXISTS cliniq.users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50)  NOT NULL UNIQUE,
    password        VARCHAR(255) NOT NULL,
    full_name       VARCHAR(100) NOT NULL,
    role            VARCHAR(20)  NOT NULL CHECK (role IN ('RECEPTIONIST', 'DOCTOR')),
    active          BOOLEAN      NOT NULL DEFAULT TRUE
);

-- ===========================
-- Patients Table
-- ===========================
CREATE TABLE IF NOT EXISTS cliniq.patients (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    phone           VARCHAR(15)  NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_patient_phone_name UNIQUE (phone, name)
);

-- ===========================
-- Visits Table (Queue entries)
-- ===========================
CREATE TABLE IF NOT EXISTS cliniq.visits (
    id              BIGSERIAL PRIMARY KEY,
    token_number    INTEGER      NOT NULL,
    visit_date      DATE         NOT NULL DEFAULT CURRENT_DATE,
    status          VARCHAR(20)  NOT NULL DEFAULT 'WAITING'
                        CHECK (status IN ('WAITING', 'IN_PROGRESS', 'COMPLETED', 'NO_SHOW')),
    patient_id      BIGINT       NOT NULL REFERENCES cliniq.patients(id),
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    called_at       TIMESTAMP,
    completed_at    TIMESTAMP,
    CONSTRAINT uk_token_date UNIQUE (token_number, visit_date)
);

-- ===========================
-- Indexes
-- ===========================
CREATE INDEX IF NOT EXISTS idx_visits_visit_date ON cliniq.visits(visit_date);
CREATE INDEX IF NOT EXISTS idx_visits_status ON cliniq.visits(status);
CREATE INDEX IF NOT EXISTS idx_visits_date_status ON cliniq.visits(visit_date, status);
CREATE INDEX IF NOT EXISTS idx_patients_phone ON cliniq.patients(phone);

-- ===========================
-- Seed Data (Default Users)
-- Password: admin123 (BCrypt encoded)
-- Password: reception123 (BCrypt encoded)
-- ===========================
INSERT INTO cliniq.users (username, password, full_name, role, active)
VALUES
    ('admin', '$2b$10$IzDSouIhKS3W6gnClUovMefvRyBB1IRcGfV4//fB4SXbXXRAN8JW.', 'Dr. Admin', 'DOCTOR', true),
    ('reception', '$2b$10$Aq35M5Dp6shXrU/8MbmUl.PgxHZcKfmFyUSIk0f3JO7VYzXFw5KXy', 'Reception Staff', 'RECEPTIONIST', true)
ON CONFLICT (username) DO NOTHING;
