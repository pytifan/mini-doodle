-- ============================================
-- Mini Doodle - Seed Data for Testing
-- Run this after the application starts
-- (Flyway migrations must have run first)
-- ============================================

-- 1. Create Users
INSERT INTO users (name, email) VALUES
    ('Alice Johnson', 'alice@example.com'),
    ('Bob Smith', 'bob@example.com'),
    ('Charlie Brown', 'charlie@example.com'),
    ('Diana Prince', 'diana@example.com'),
    ('Eve Wilson', 'eve@example.com');

-- 2. Create Calendars (one per user)
INSERT INTO calendars (user_id, timezone) VALUES
    (1, 'Europe/Warsaw'),
    (2, 'Europe/Warsaw'),
    (3, 'UTC'),
    (4, 'America/New_York'),
    (5, 'Europe/London');

-- 3. Create Time Slots for Alice (user 1, calendar 1)
--    Mix of free and busy, spread across a week
INSERT INTO time_slots (calendar_id, start_time, end_time, status) VALUES
    -- Monday
    (1, '2025-03-03 09:00:00', '2025-03-03 10:00:00', 'FREE'),
    (1, '2025-03-03 10:00:00', '2025-03-03 11:00:00', 'FREE'),
    (1, '2025-03-03 13:00:00', '2025-03-03 14:00:00', 'BUSY'),
    (1, '2025-03-03 14:00:00', '2025-03-03 15:30:00', 'FREE'),
    -- Tuesday
    (1, '2025-03-04 09:00:00', '2025-03-04 09:30:00', 'FREE'),
    (1, '2025-03-04 10:00:00', '2025-03-04 11:00:00', 'BUSY'),
    (1, '2025-03-04 14:00:00', '2025-03-04 16:00:00', 'FREE'),
    -- Wednesday
    (1, '2025-03-05 08:00:00', '2025-03-05 09:00:00', 'FREE'),
    (1, '2025-03-05 11:00:00', '2025-03-05 12:00:00', 'FREE'),
    (1, '2025-03-05 15:00:00', '2025-03-05 17:00:00', 'FREE');

-- 4. Create Time Slots for Bob (user 2, calendar 2)
INSERT INTO time_slots (calendar_id, start_time, end_time, status) VALUES
    (2, '2025-03-03 09:00:00', '2025-03-03 10:00:00', 'FREE'),
    (2, '2025-03-03 11:00:00', '2025-03-03 12:00:00', 'FREE'),
    (2, '2025-03-04 10:00:00', '2025-03-04 11:30:00', 'FREE'),
    (2, '2025-03-04 14:00:00', '2025-03-04 15:00:00', 'BUSY'),
    (2, '2025-03-05 09:00:00', '2025-03-05 10:00:00', 'FREE');

-- 5. Create Time Slots for Charlie (user 3, calendar 3)
INSERT INTO time_slots (calendar_id, start_time, end_time, status) VALUES
    (3, '2025-03-03 08:00:00', '2025-03-03 09:00:00', 'FREE'),
    (3, '2025-03-03 13:00:00', '2025-03-03 14:30:00', 'FREE'),
    (3, '2025-03-04 10:00:00', '2025-03-04 11:00:00', 'FREE');

-- 6. Book meetings on some BUSY slots
--    Alice's Monday 13:00-14:00 slot (id=3) is BUSY -> add a meeting
INSERT INTO meetings (time_slot_id, title, description, organizer_id) VALUES
    (3, 'Sprint Retrospective', 'Review of last sprint achievements and improvements', 1);

INSERT INTO meeting_participants (meeting_id, user_id) VALUES
    (1, 2),  -- Bob
    (1, 3);  -- Charlie

--    Alice's Tuesday 10:00-11:00 slot (id=6) is BUSY -> add a meeting
INSERT INTO meetings (time_slot_id, title, description, organizer_id) VALUES
    (6, '1:1 with Bob', 'Weekly sync with Bob about project progress', 1);

INSERT INTO meeting_participants (meeting_id, user_id) VALUES
    (2, 2);  -- Bob

--    Bob's Tuesday 14:00-15:00 slot (id=14) is BUSY -> add a meeting
INSERT INTO meetings (time_slot_id, title, description, organizer_id) VALUES
    (14, 'Design Review', 'Review new API design proposals', 2);

INSERT INTO meeting_participants (meeting_id, user_id) VALUES
    (3, 1),  -- Alice
    (3, 4);  -- Diana

-- ============================================
-- Quick verification queries
-- ============================================

-- Check all users
-- SELECT * FROM users;

-- Check Alice's slots with meeting info
-- SELECT ts.id, ts.start_time, ts.end_time, ts.status, m.title as meeting_title
-- FROM time_slots ts
-- LEFT JOIN meetings m ON m.time_slot_id = ts.id
-- WHERE ts.calendar_id = 1
-- ORDER BY ts.start_time;

-- Check Alice's availability for Monday
-- SELECT ts.start_time, ts.end_time, ts.status, m.title
-- FROM time_slots ts
-- LEFT JOIN meetings m ON m.time_slot_id = ts.id
-- WHERE ts.calendar_id = 1
--   AND ts.start_time >= '2025-03-03 00:00:00'
--   AND ts.end_time <= '2025-03-03 23:59:59'
-- ORDER BY ts.start_time;
