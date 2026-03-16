-- =========================================
-- CliniQ Schema Migration: Sprint 2 - US6 Enhanced
-- Per-Day Schedules, Multiple Breaks, Holidays
-- Run against database: msc, schema: cliniq
-- Prerequisite: schema-alter1.sql, schema-alter2.sql
-- =========================================

SET search_path TO cliniq;

-- ===========================
-- Day Schedules Table
-- Per-day working hour configuration
-- ===========================
CREATE TABLE IF NOT EXISTS cliniq.day_schedules (
    id              BIGSERIAL PRIMARY KEY,
    day_of_week     VARCHAR(10)  NOT NULL UNIQUE
                        CHECK (day_of_week IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY')),
    start_time      TIME         NOT NULL DEFAULT '08:00',
    end_time        TIME         NOT NULL DEFAULT '17:00',
    is_working_day  BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ===========================
-- Day Schedule Breaks Table
-- Multiple breaks per day
-- ===========================
CREATE TABLE IF NOT EXISTS cliniq.day_schedule_breaks (
    id                  BIGSERIAL PRIMARY KEY,
    day_schedule_id     BIGINT       NOT NULL REFERENCES cliniq.day_schedules(id) ON DELETE CASCADE,
    break_start         TIME         NOT NULL,
    break_end           TIME         NOT NULL,
    label               VARCHAR(50)  DEFAULT 'Break',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===========================
-- Clinic Holidays Table
-- Specific date holidays/day-offs
-- ===========================
CREATE TABLE IF NOT EXISTS cliniq.clinic_holidays (
    id              BIGSERIAL PRIMARY KEY,
    holiday_date    DATE         NOT NULL UNIQUE,
    description     VARCHAR(200) DEFAULT '',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ===========================
-- Indexes
-- ===========================
CREATE INDEX IF NOT EXISTS idx_day_schedules_day ON cliniq.day_schedules(day_of_week);
CREATE INDEX IF NOT EXISTS idx_day_schedule_breaks_schedule ON cliniq.day_schedule_breaks(day_schedule_id);
CREATE INDEX IF NOT EXISTS idx_clinic_holidays_date ON cliniq.clinic_holidays(holiday_date);

-- ===========================
-- Seed Data: Default Day Schedules (Mon-Fri working, Sat-Sun off)
-- ===========================
INSERT INTO cliniq.day_schedules (day_of_week, start_time, end_time, is_working_day)
VALUES
    ('MONDAY',    '08:00', '17:00', TRUE),
    ('TUESDAY',   '08:00', '17:00', TRUE),
    ('WEDNESDAY', '08:00', '17:00', TRUE),
    ('THURSDAY',  '08:00', '17:00', TRUE),
    ('FRIDAY',    '08:00', '17:00', TRUE),
    ('SATURDAY',  '09:00', '13:00', FALSE),
    ('SUNDAY',    '09:00', '13:00', FALSE)
ON CONFLICT (day_of_week) DO NOTHING;

-- Seed default lunch breaks for working days
INSERT INTO cliniq.day_schedule_breaks (day_schedule_id, break_start, break_end, label)
SELECT ds.id, '12:00'::TIME, '13:00'::TIME, 'Lunch Break'
FROM cliniq.day_schedules ds
WHERE ds.is_working_day = TRUE
AND NOT EXISTS (
    SELECT 1 FROM cliniq.day_schedule_breaks dsb WHERE dsb.day_schedule_id = ds.id
);

-- ===========================
-- Day Overrides Table
-- Date-specific overrides for the weekly schedule.
-- e.g. mark a specific Wednesday as non-working,
-- or make a particular Sunday a working day with custom hours.
-- ===========================
CREATE TABLE IF NOT EXISTS cliniq.day_overrides (
    id              BIGSERIAL PRIMARY KEY,
    override_date   DATE         NOT NULL UNIQUE,
    is_working_day  BOOLEAN      NOT NULL DEFAULT FALSE,
    start_time      TIME,
    end_time        TIME,
    reason          VARCHAR(200) DEFAULT '',
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_day_overrides_date ON cliniq.day_overrides(override_date);

-- ===========================
-- Visits Table: Add columns for queue merge (US8)
-- visit_type: WALK_IN or APPOINTMENT
-- appointment_id: link to appointment when checked in
-- appointment_time: the slot start time (used for priority ordering)
-- ===========================
ALTER TABLE cliniq.visits ADD COLUMN IF NOT EXISTS visit_type VARCHAR(20) NOT NULL DEFAULT 'WALK_IN'
    CHECK (visit_type IN ('WALK_IN', 'APPOINTMENT'));
ALTER TABLE cliniq.visits ADD COLUMN IF NOT EXISTS appointment_id BIGINT REFERENCES cliniq.appointments(id);
ALTER TABLE cliniq.visits ADD COLUMN IF NOT EXISTS appointment_time TIME;

CREATE INDEX IF NOT EXISTS idx_visits_appointment ON cliniq.visits(appointment_id);
CREATE INDEX IF NOT EXISTS idx_visits_type_status ON cliniq.visits(visit_type, status, visit_date);
