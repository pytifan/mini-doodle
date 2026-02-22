-- Users table
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255)        NOT NULL,
    email       VARCHAR(255) UNIQUE NOT NULL,
    created_at  TIMESTAMP           NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP           NOT NULL DEFAULT NOW()
);

-- Calendar table (domain concept - each user has exactly one)
CREATE TABLE calendars (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT UNIQUE NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    timezone    VARCHAR(50)   NOT NULL DEFAULT 'UTC',
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Time slots table
CREATE TABLE time_slots (
    id          BIGSERIAL PRIMARY KEY,
    calendar_id BIGINT        NOT NULL REFERENCES calendars(id) ON DELETE CASCADE,
    start_time  TIMESTAMP     NOT NULL,
    end_time    TIMESTAMP     NOT NULL,
    status      VARCHAR(10)   NOT NULL DEFAULT 'FREE' CHECK (status IN ('FREE', 'BUSY')),
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT  chk_time_range CHECK (end_time > start_time)
);

-- Meetings table
CREATE TABLE meetings (
    id          BIGSERIAL PRIMARY KEY,
    time_slot_id BIGINT       NOT NULL REFERENCES time_slots(id) ON DELETE CASCADE,
    title       VARCHAR(255)  NOT NULL,
    description TEXT,
    organizer_id BIGINT       NOT NULL REFERENCES users(id),
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Meeting participants (many-to-many)
CREATE TABLE meeting_participants (
    meeting_id  BIGINT NOT NULL REFERENCES meetings(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    PRIMARY KEY (meeting_id, user_id)
);

-- Performance indexes
CREATE INDEX idx_time_slots_calendar_id ON time_slots(calendar_id);
CREATE INDEX idx_time_slots_status ON time_slots(status);
CREATE INDEX idx_time_slots_time_range ON time_slots(start_time, end_time);
CREATE INDEX idx_time_slots_calendar_status_time ON time_slots(calendar_id, status, start_time, end_time);
CREATE INDEX idx_meetings_time_slot_id ON meetings(time_slot_id);
CREATE INDEX idx_meetings_organizer_id ON meetings(organizer_id);
CREATE INDEX idx_meeting_participants_user_id ON meeting_participants(user_id);
CREATE INDEX idx_calendars_user_id ON calendars(user_id);
