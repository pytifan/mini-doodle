package com.minidoodle.service;

import com.minidoodle.domain.Calendar;
import com.minidoodle.domain.SlotStatus;
import com.minidoodle.domain.TimeSlot;
import com.minidoodle.dto.AvailabilityDto;
import com.minidoodle.dto.TimeSlotDto;
import com.minidoodle.exception.BusinessRuleException;
import com.minidoodle.exception.ResourceNotFoundException;
import com.minidoodle.repository.CalendarRepository;
import com.minidoodle.repository.TimeSlotRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TimeSlotService {

    private static final Logger log = LoggerFactory.getLogger(TimeSlotService.class);

    private final TimeSlotRepository timeSlotRepository;
    private final CalendarRepository calendarRepository;
    private final UserService userService;

    @Transactional
    public TimeSlotDto.Response createSlot(Long userId, TimeSlotDto.CreateRequest request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());

        Calendar calendar = findCalendarByUserId(userId);

        if (timeSlotRepository.existsOverlappingSlot(calendar.getId(), request.getStartTime(), request.getEndTime())) {
            throw new BusinessRuleException("Time slot overlaps with an existing slot");
        }

        TimeSlot slot = TimeSlot.builder()
                .calendar(calendar)
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .status(SlotStatus.FREE)
                .build();

        slot = timeSlotRepository.save(slot);
        log.info("Created time slot {} for user {}", slot.getId(), userId);
        return toResponse(slot);
    }

    @Transactional(readOnly = true)
    public TimeSlotDto.Response getSlot(Long userId, Long slotId) {
        Calendar calendar = findCalendarByUserId(userId);
        TimeSlot slot = findSlotOrThrow(slotId, calendar.getId());
        return toResponse(slot);
    }

    @Transactional(readOnly = true)
    public Page<TimeSlotDto.Response> listSlots(Long userId, SlotStatus status,
                                                 LocalDateTime from, LocalDateTime to,
                                                 Pageable pageable) {
        Calendar calendar = findCalendarByUserId(userId);

        Page<TimeSlot> slots;

        if (status != null && from != null && to != null) {
            slots = timeSlotRepository.findByCalendarIdAndStatusAndTimeRange(
                    calendar.getId(), status, from, to, pageable);
        } else if (status != null) {
            slots = timeSlotRepository.findByCalendarIdAndStatus(calendar.getId(), status, pageable);
        } else if (from != null && to != null) {
            slots = timeSlotRepository.findByCalendarIdAndTimeRange(calendar.getId(), from, to, pageable);
        } else {
            slots = timeSlotRepository.findByCalendarId(calendar.getId(), pageable);
        }

        return slots.map(this::toResponse);
    }

    @Transactional
    public TimeSlotDto.Response updateSlot(Long userId, Long slotId, TimeSlotDto.UpdateRequest request) {
        validateTimeRange(request.getStartTime(), request.getEndTime());

        Calendar calendar = findCalendarByUserId(userId);
        TimeSlot slot = findSlotOrThrow(slotId, calendar.getId());

        if (slot.hasMeeting()) {
            throw new BusinessRuleException("Cannot modify a slot that has a meeting booked. Cancel the meeting first.");
        }

        if (timeSlotRepository.existsOverlappingSlot(calendar.getId(), request.getStartTime(), request.getEndTime(), slotId)) {
            throw new BusinessRuleException("Updated time slot would overlap with an existing slot");
        }

        slot.setStartTime(request.getStartTime());
        slot.setEndTime(request.getEndTime());
        slot = timeSlotRepository.save(slot);

        log.info("Updated time slot {} for user {}", slotId, userId);
        return toResponse(slot);
    }

    @Transactional
    public TimeSlotDto.Response updateSlotStatus(Long userId, Long slotId, TimeSlotDto.StatusRequest request) {
        Calendar calendar = findCalendarByUserId(userId);
        TimeSlot slot = findSlotOrThrow(slotId, calendar.getId());

        if (slot.hasMeeting() && request.getStatus() == SlotStatus.FREE) {
            throw new BusinessRuleException("Cannot mark a slot as FREE while it has a meeting. Cancel the meeting first.");
        }

        slot.setStatus(request.getStatus());
        slot = timeSlotRepository.save(slot);

        log.info("Updated slot {} status to {} for user {}", slotId, request.getStatus(), userId);
        return toResponse(slot);
    }

    @Transactional
    public void deleteSlot(Long userId, Long slotId) {
        Calendar calendar = findCalendarByUserId(userId);
        TimeSlot slot = findSlotOrThrow(slotId, calendar.getId());

        if (slot.hasMeeting()) {
            throw new BusinessRuleException("Cannot delete a slot that has a meeting booked. Cancel the meeting first.");
        }

        timeSlotRepository.delete(slot);
        log.info("Deleted time slot {} for user {}", slotId, userId);
    }

    @Transactional(readOnly = true)
    public AvailabilityDto getAvailability(Long userId, LocalDateTime from, LocalDateTime to) {
        var user = userService.findUserOrThrow(userId);
        Calendar calendar = findCalendarByUserId(userId);

        List<TimeSlot> slots = timeSlotRepository.findAllByCalendarIdAndTimeRange(
                calendar.getId(), from, to);

        long freeCount = slots.stream().filter(TimeSlot::isFree).count();
        long busyCount = slots.stream().filter(TimeSlot::isBusy).count();

        List<AvailabilityDto.SlotSummary> summaries = slots.stream()
                .map(slot -> AvailabilityDto.SlotSummary.builder()
                        .slotId(slot.getId())
                        .startTime(slot.getStartTime())
                        .endTime(slot.getEndTime())
                        .durationMinutes(Duration.between(slot.getStartTime(), slot.getEndTime()).toMinutes())
                        .status(slot.getStatus())
                        .meetingTitle(slot.getMeeting() != null ? slot.getMeeting().getTitle() : null)
                        .build())
                .toList();

        return AvailabilityDto.builder()
                .userId(userId)
                .userName(user.getName())
                .from(from)
                .to(to)
                .totalSlots(slots.size())
                .freeSlots((int) freeCount)
                .busySlots((int) busyCount)
                .slots(summaries)
                .build();
    }

    public TimeSlot findSlotEntityOrThrow(Long slotId, Long calendarId) {
        return findSlotOrThrow(slotId, calendarId);
    }

    Calendar findCalendarByUserId(Long userId) {
        // Verify user exists
        userService.findUserOrThrow(userId);
        return calendarRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Calendar not found for user: " + userId));
    }

    private TimeSlot findSlotOrThrow(Long slotId, Long calendarId) {
        return timeSlotRepository.findByIdAndCalendarId(slotId, calendarId)
                .orElseThrow(() -> new ResourceNotFoundException("TimeSlot", slotId));
    }

    private void validateTimeRange(LocalDateTime start, LocalDateTime end) {
        if (!end.isAfter(start)) {
            throw new BusinessRuleException("End time must be after start time");
        }
    }

    private TimeSlotDto.Response toResponse(TimeSlot slot) {
        return TimeSlotDto.Response.builder()
                .id(slot.getId())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .durationMinutes(Duration.between(slot.getStartTime(), slot.getEndTime()).toMinutes())
                .status(slot.getStatus())
                .hasMeeting(slot.getMeeting() != null)
                .meetingId(slot.getMeeting() != null ? slot.getMeeting().getId() : null)
                .createdAt(slot.getCreatedAt() != null ? slot.getCreatedAt().toString() : null)
                .build();
    }
}
