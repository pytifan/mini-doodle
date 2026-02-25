-- Optimistic locking: version column for time_slots to prevent concurrent booking races
ALTER TABLE time_slots ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Enforce at DB level that a time slot can only have one meeting
ALTER TABLE meetings ADD CONSTRAINT uq_meetings_time_slot_id UNIQUE (time_slot_id);
