-- =========================================
-- CliniQ Schema Migration: Sprint 2 - US6
-- Appointment Slots & Working Hours
-- Run against database: msc, schema: cliniq
-- =========================================

SET search_path TO cliniq;

-- ===========================
-- Clinic Settings Table
-- ===========================
CREATE TABLE IF NOT EXISTS cliniq.clinic_settings (
    id                      BIGSERIAL PRIMARY KEY,
    clinic_name             VARCHAR(100) NOT NULL,
    working_start_time      TIME         NOT NULL DEFAULT '08:00',
    working_end_time        TIME         NOT NULL DEFAULT '17:00',
    slot_duration_minutes   INTEGER      NOT NULL DEFAULT 15,
    max_patients_per_slot   INTEGER      NOT NULL DEFAULT 1,
    break_start_time        TIME,
    break_end_time          TIME,
    updated_at              TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ===========================
-- Appointment Slots Table
-- ===========================
CREATE TABLE IF NOT EXISTS cliniq.appointment_slots (
    id              BIGSERIAL PRIMARY KEY,
    day_of_week     VARCHAR(10)  NOT NULL
                        CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    start_time      TIME         NOT NULL,
    end_time        TIME         NOT NULL,
    max_patients    INTEGER      NOT NULL DEFAULT 1,
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_slot_day_time UNIQUE (day_of_week, start_time)
);

-- ===========================
-- Indexes
-- ===========================
CREATE INDEX IF NOT EXISTS idx_slots_day_of_week ON cliniq.appointment_slots(day_of_week);
CREATE INDEX IF NOT EXISTS idx_slots_active ON cliniq.appointment_slots(is_active);

-- ===========================
-- Seed Data (Default Clinic Settings)
-- ===========================
INSERT INTO cliniq.clinic_settings (clinic_name, working_start_time, working_end_time, slot_duration_minutes, max_patients_per_slot, break_start_time, break_end_time)
SELECT 'Clinic Queue', '08:00', '17:00', 15, 1, '12:00', '13:00'
WHERE NOT EXISTS (SELECT 1 FROM cliniq.clinic_settings);
