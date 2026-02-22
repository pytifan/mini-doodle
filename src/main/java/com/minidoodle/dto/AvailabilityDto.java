package com.minidoodle.dto;

import com.minidoodle.domain.SlotStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Aggregated availability view for a time frame")
public class AvailabilityDto {

    @Schema(description = "User ID")
    private Long userId;

    @Schema(description = "User name")
    private String userName;

    @Schema(description = "Start of the queried time frame")
    private LocalDateTime from;

    @Schema(description = "End of the queried time frame")
    private LocalDateTime to;

    @Schema(description = "Total number of slots in the time frame")
    private int totalSlots;

    @Schema(description = "Number of free slots")
    private int freeSlots;

    @Schema(description = "Number of busy slots")
    private int busySlots;

    @Schema(description = "Individual slot details")
    private List<SlotSummary> slots;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Summary of a single slot in the availability view")
    public static class SlotSummary {

        @Schema(description = "Slot ID")
        private Long slotId;

        @Schema(description = "Start time")
        private LocalDateTime startTime;

        @Schema(description = "End time")
        private LocalDateTime endTime;

        @Schema(description = "Duration in minutes")
        private long durationMinutes;

        @Schema(description = "Status")
        private SlotStatus status;

        @Schema(description = "Meeting title if booked")
        private String meetingTitle;
    }
}
