package com.minidoodle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class MeetingDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Request to create a meeting from a time slot")
    public static class CreateRequest {

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        @Schema(description = "Meeting title", example = "Sprint Planning")
        private String title;

        @Schema(description = "Meeting description", example = "Q1 sprint planning session")
        private String description;

        @Schema(description = "List of participant user IDs", example = "[2, 3]")
        private List<Long> participantIds;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Request to update a meeting")
    public static class UpdateRequest {

        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        @Schema(description = "Meeting title", example = "Updated Sprint Planning")
        private String title;

        @Schema(description = "Meeting description", example = "Updated description")
        private String description;

        @Schema(description = "Updated list of participant user IDs", example = "[2, 3, 4]")
        private List<Long> participantIds;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Meeting response")
    public static class Response {

        @Schema(description = "Meeting ID")
        private Long id;

        @Schema(description = "Meeting title")
        private String title;

        @Schema(description = "Meeting description")
        private String description;

        @Schema(description = "Start time")
        private LocalDateTime startTime;

        @Schema(description = "End time")
        private LocalDateTime endTime;

        @Schema(description = "Duration in minutes")
        private long durationMinutes;

        @Schema(description = "Organizer details")
        private UserDto.Response organizer;

        @Schema(description = "Participant list")
        private Set<UserDto.Response> participants;

        @Schema(description = "Associated time slot ID")
        private Long timeSlotId;

        @Schema(description = "Timestamp of creation")
        private String createdAt;
    }
}
