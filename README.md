# Mini Doodle - Meeting Scheduling Platform

A high-performance meeting scheduling service built with Spring Boot, inspired by Doodle.

## Overview

Mini Doodle enables users to manage their time slots, schedule meetings, and view calendar availability. Users can define available slots which can be converted into meetings, with full support for querying free/busy status across time frames.

## Architecture

### Domain Model

```
User ─> Calendar ─> TimeSlot ─> Meeting
                                     |
                                  Participant
```

- **User**: Platform users who own calendars
- **Calendar**: Personal calendar belonging to a user (domain-only concept)
- **TimeSlot**: Configurable time blocks that can be free or busy
- **Meeting**: A booked time slot with title, description, and participants

### Design Decisions

1. **Calendar as Domain Concept**: The `Calendar` entity exists only in the domain layer, not exposed directly via REST. Users interact with slots and meetings through their calendar implicitly.

2. **Layered Architecture**: Controller → Service → Repository pattern with clear separation of concerns.

3. **Performance Considerations**:
   - Database indexing on frequently queried columns (user_id, start_time, end_time, status)
   - Pagination on list endpoints
   - Composite indexes for time-range queries
   - Connection pooling via HikariCP

4. **Data Persistence**: PostgreSQL for relational data integrity with proper foreign key constraints.

## Tech Stack

| Component        | Technology               |
|-----------------|--------------------------|
| Framework       | Spring Boot 3.2          |
| Language        | Java 21                  |
| Database        | PostgreSQL 16            |
| Build Tool      | Maven                    |
| Containerization| Docker + Docker Compose  |
| Documentation   | SpringDoc OpenAPI (Swagger) |
| Metrics         | Spring Boot Actuator + Micrometer |
| Testing         | JUnit 5 + Testcontainers |

## Getting Started

### Prerequisites

- Docker & Docker Compose

### Running the Application

```bash
docker compose up --build
```

The service will be available at `http://localhost:8080`.

### API Documentation

Once running, access the Swagger UI:
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs

### Metrics & Health

- **Health Check**: http://localhost:8080/actuator/health
- **Metrics**: http://localhost:8080/actuator/metrics
- **Prometheus**: http://localhost:8080/actuator/prometheus

## API Endpoints

### Users

| Method | Endpoint              | Description          |
|--------|-----------------------|----------------------|
| POST   | `/api/v1/users`       | Create a new user    |
| GET    | `/api/v1/users`       | List all users       |
| GET    | `/api/v1/users/{id}`  | Get user by ID       |

### Time Slots

| Method | Endpoint                                    | Description                           |
|--------|---------------------------------------------|---------------------------------------|
| POST   | `/api/v1/users/{userId}/slots`              | Create a time slot                    |
| GET    | `/api/v1/users/{userId}/slots`              | List user's slots (with filters)      |
| GET    | `/api/v1/users/{userId}/slots/{slotId}`     | Get specific slot                     |
| PUT    | `/api/v1/users/{userId}/slots/{slotId}`     | Update a time slot                    |
| PATCH  | `/api/v1/users/{userId}/slots/{slotId}/status` | Change slot status (FREE/BUSY)    |
| DELETE | `/api/v1/users/{userId}/slots/{slotId}`     | Delete a time slot                    |
| GET    | `/api/v1/users/{userId}/availability`       | Query free/busy aggregated view       |

### Meetings

| Method | Endpoint                                              | Description              |
|--------|-------------------------------------------------------|--------------------------|
| POST   | `/api/v1/users/{userId}/slots/{slotId}/meetings`      | Book a meeting from slot |
| GET    | `/api/v1/users/{userId}/meetings`                     | List user's meetings     |
| GET    | `/api/v1/users/{userId}/meetings/{meetingId}`         | Get meeting details      |
| PUT    | `/api/v1/users/{userId}/meetings/{meetingId}`         | Update meeting           |
| DELETE | `/api/v1/users/{userId}/meetings/{meetingId}`         | Cancel meeting           |

### Query Parameters

**Slot listing** supports:
- `status` - Filter by FREE or BUSY
- `from` / `to` - Filter by time range (ISO 8601)
- `page` / `size` - Pagination

**Availability** supports:
- `from` / `to` - Required time frame (ISO 8601)

## Example Usage

### 1. Create a User
```bash
curl -X POST http://localhost:8080/api/v1/users \
  -H "Content-Type: application/json" \
  -d '{"name": "Alice", "email": "alice@example.com"}'
```

### 2. Create a Time Slot
```bash
curl -X POST http://localhost:8080/api/v1/users/1/slots \
  -H "Content-Type: application/json" \
  -d '{
    "startTime": "2025-03-01T09:00:00",
    "endTime": "2025-03-01T10:00:00"
  }'
```

### 3. Book a Meeting
```bash
curl -X POST http://localhost:8080/api/v1/users/1/slots/1/meetings \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Sprint Planning",
    "description": "Q1 sprint planning session",
    "participantIds": [2, 3]
  }'
```

### 4. Query Availability
```bash
curl "http://localhost:8080/api/v1/users/1/availability?from=2025-03-01T00:00:00&to=2025-03-01T23:59:59"
```

## Testing

Run tests locally (requires Maven and Docker for Testcontainers):
```bash
mvn test
```

## Project Structure

```
src/main/java/com/minidoodle/
├── config/          # Configuration classes
├── controller/      # REST controllers
├── domain/          # Entity classes (Calendar lives here)
├── dto/             # Request/Response DTOs
├── exception/       # Custom exceptions & handlers
├── repository/      # Spring Data JPA repositories
└── service/         # Business logic
```