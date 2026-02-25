package com.minidoodle.controller;

import com.minidoodle.domain.SlotStatus;
import com.minidoodle.dto.AvailabilityDto;
import com.minidoodle.dto.TimeSlotDto;
import com.minidoodle.service.TimeSlotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/users/{userId}")
@RequiredArgsConstructor
@Tag(name = "Time Slots", description = "Time slot management endpoints")
public class TimeSlotController {

    private final TimeSlotService timeSlotService;

    @PostMapping("/slots")
    @Operation(summary = "Create a time slot", description = "Creates a new available time slot in the user's calendar")
    public ResponseEntity<TimeSlotDto.Response> createSlot(
            @PathVariable Long userId,
            @Valid @RequestBody TimeSlotDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(timeSlotService.createSlot(userId, request));
    }

    @GetMapping("/slots")
    @Operation(summary = "List time slots", description = "Returns paginated slots with optional filters")
    public ResponseEntity<Page<TimeSlotDto.Response>> listSlots(
            @PathVariable Long userId,
            @RequestParam(required = false) @Parameter(description = "Filter by status") SlotStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "Start of time range (ISO 8601)") LocalDateTime from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "End of time range (ISO 8601)") LocalDateTime to,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity
                .ok(timeSlotService.listSlots(userId, status, from, to, pageable));
    }

    @GetMapping("/slots/{slotId}")
    @Operation(summary = "Get a time slot")
    public ResponseEntity<TimeSlotDto.Response> getSlot(
            @PathVariable Long userId,
            @PathVariable Long slotId) {
        return ResponseEntity
                .ok(timeSlotService.getSlot(userId, slotId));
    }

    @PutMapping("/slots/{slotId}")
    @Operation(summary = "Update a time slot", description = "Updates the time range. Cannot modify slots with meetings.")
    public ResponseEntity<TimeSlotDto.Response> updateSlot(
            @PathVariable Long userId,
            @PathVariable Long slotId,
            @Valid @RequestBody TimeSlotDto.UpdateRequest request) {
        return ResponseEntity
                .ok(timeSlotService.updateSlot(userId, slotId, request));
    }

    @PatchMapping("/slots/{slotId}/status")
    @Operation(summary = "Change slot status", description = "Mark a slot as FREE or BUSY")
    public ResponseEntity<TimeSlotDto.Response> updateSlotStatus(
            @PathVariable Long userId,
            @PathVariable Long slotId,
            @Valid @RequestBody TimeSlotDto.StatusRequest request) {
        return ResponseEntity
                .ok(timeSlotService.updateSlotStatus(userId, slotId, request));
    }

    @DeleteMapping("/slots/{slotId}")
    @Operation(summary = "Delete a time slot", description = "Removes a slot. Cannot delete slots with meetings.")
    public ResponseEntity<Void> deleteSlot(
            @PathVariable Long userId,
            @PathVariable Long slotId) {
        timeSlotService.deleteSlot(userId, slotId);
        return ResponseEntity
                .noContent().build();
    }

    @GetMapping("/availability")
    @Operation(summary = "Query availability",
            description = "Returns aggregated free/busy view for the given time frame")
    public ResponseEntity<AvailabilityDto> getAvailability(
            @PathVariable Long userId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "Start of time frame (ISO 8601)", required = true) LocalDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            @Parameter(description = "End of time frame (ISO 8601)", required = true) LocalDateTime to) {
        return ResponseEntity
                .ok(timeSlotService.getAvailability(userId, from, to));
    }
}
