-- =========================================
-- CliniQ Schema Migration: Sprint 2 - US7
-- Appointment Booking
-- Run against database: msc, schema: cliniq
-- Prerequisite: schema-alter1.sql
-- =========================================

SET search_path TO cliniq;

-- ===========================
-- Appointments Table
-- ===========================
CREATE TABLE IF NOT EXISTS cliniq.appointments (
    id                  BIGSERIAL PRIMARY KEY,
    patient_id          BIGINT       NOT NULL REFERENCES cliniq.patients(id),
    slot_id             BIGINT       NOT NULL REFERENCES cliniq.appointment_slots(id),
    appointment_date    DATE         NOT NULL,
    start_time          TIME         NOT NULL,
    end_time            TIME         NOT NULL,
    token_number        INTEGER,
    status              VARCHAR(20)  NOT NULL DEFAULT 'BOOKED'
                            CHECK (status IN ('BOOKED', 'CHECKED_IN', 'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    notes               TEXT,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    cancelled_at        TIMESTAMP,
    CONSTRAINT uk_patient_appointment_date UNIQUE (patient_id, appointment_date)
);

-- ===========================
-- Indexes
-- ===========================
CREATE INDEX IF NOT EXISTS idx_appointments_date ON cliniq.appointments(appointment_date);
CREATE INDEX IF NOT EXISTS idx_appointments_status ON cliniq.appointments(status);
CREATE INDEX IF NOT EXISTS idx_appointments_date_status ON cliniq.appointments(appointment_date, status);
CREATE INDEX IF NOT EXISTS idx_appointments_patient ON cliniq.appointments(patient_id);
CREATE INDEX IF NOT EXISTS idx_appointments_slot ON cliniq.appointments(slot_id);
