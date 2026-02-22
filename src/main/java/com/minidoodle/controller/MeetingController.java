package com.minidoodle.controller;

import com.minidoodle.dto.MeetingDto;
import com.minidoodle.service.MeetingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users/{userId}")
@RequiredArgsConstructor
@Tag(name = "Meetings", description = "Meeting scheduling endpoints")
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping("/slots/{slotId}/meetings")
    @Operation(summary = "Book a meeting", description = "Converts an available slot into a meeting")
    public ResponseEntity<MeetingDto.Response> createMeeting(
            @PathVariable Long userId,
            @PathVariable Long slotId,
            @Valid @RequestBody MeetingDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(meetingService.createMeeting(userId, slotId, request));
    }

    @GetMapping("/meetings")
    @Operation(summary = "List meetings", description = "Returns paginated list of user's meetings")
    public ResponseEntity<Page<MeetingDto.Response>> listMeetings(
            @PathVariable Long userId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(meetingService.listMeetings(userId, pageable));
    }

    @GetMapping("/meetings/{meetingId}")
    @Operation(summary = "Get meeting details")
    public ResponseEntity<MeetingDto.Response> getMeeting(
            @PathVariable Long userId,
            @PathVariable Long meetingId) {
        return ResponseEntity.ok(meetingService.getMeeting(userId, meetingId));
    }

    @PutMapping("/meetings/{meetingId}")
    @Operation(summary = "Update a meeting", description = "Updates meeting title, description, and participants")
    public ResponseEntity<MeetingDto.Response> updateMeeting(
            @PathVariable Long userId,
            @PathVariable Long meetingId,
            @Valid @RequestBody MeetingDto.UpdateRequest request) {
        return ResponseEntity.ok(meetingService.updateMeeting(userId, meetingId, request));
    }

    @DeleteMapping("/meetings/{meetingId}")
    @Operation(summary = "Cancel a meeting", description = "Cancels a meeting and frees the associated time slot")
    public ResponseEntity<Void> cancelMeeting(
            @PathVariable Long userId,
            @PathVariable Long meetingId) {
        meetingService.cancelMeeting(userId, meetingId);
        return ResponseEntity.noContent().build();
    }
}
