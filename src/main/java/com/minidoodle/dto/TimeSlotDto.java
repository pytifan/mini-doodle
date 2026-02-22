package com.minidoodle.dto;

import com.minidoodle.domain.SlotStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

public class TimeSlotDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Request to create a time slot")
    public static class CreateRequest {

        @NotNull(message = "Start time is required")
        @Schema(description = "Start time of the slot", example = "2025-03-01T09:00:00")
        private LocalDateTime startTime;

        @NotNull(message = "End time is required")
        @Schema(description = "End time of the slot", example = "2025-03-01T10:00:00")
        private LocalDateTime endTime;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Request to update a time slot")
    public static class UpdateRequest {

        @NotNull(message = "Start time is required")
        @Schema(description = "New start time of the slot", example = "2025-03-01T10:00:00")
        private LocalDateTime startTime;

        @NotNull(message = "End time is required")
        @Schema(description = "New end time of the slot", example = "2025-03-01T11:00:00")
        private LocalDateTime endTime;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Request to change slot status")
    public static class StatusRequest {

        @NotNull(message = "Status is required")
        @Schema(description = "New status for the slot", example = "BUSY")
        private SlotStatus status;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Time slot response")
    public static class Response {

        @Schema(description = "Slot ID")
        private Long id;

        @Schema(description = "Start time")
        private LocalDateTime startTime;

        @Schema(description = "End time")
        private LocalDateTime endTime;

        @Schema(description = "Duration in minutes")
        private long durationMinutes;

        @Schema(description = "Slot status (FREE or BUSY)")
        private SlotStatus status;

        @Schema(description = "Whether the slot has a meeting booked")
        private boolean hasMeeting;

        @Schema(description = "Meeting ID if booked")
        private Long meetingId;

        @Schema(description = "Timestamp of creation")
        private String createdAt;
    }
}
