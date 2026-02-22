package com.minidoodle.service;

import com.minidoodle.domain.*;
import com.minidoodle.dto.MeetingDto;
import com.minidoodle.dto.UserDto;
import com.minidoodle.exception.BusinessRuleException;
import com.minidoodle.exception.ResourceNotFoundException;
import com.minidoodle.repository.MeetingRepository;
import com.minidoodle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private static final Logger log = LoggerFactory.getLogger(MeetingService.class);

    private final MeetingRepository meetingRepository;
    private final UserRepository userRepository;
    private final TimeSlotService timeSlotService;
    private final UserService userService;

    @Transactional
    public MeetingDto.Response createMeeting(Long userId, Long slotId, MeetingDto.CreateRequest request) {
        User organizer = userService.findUserOrThrow(userId);
        Calendar calendar = timeSlotService.findCalendarByUserId(userId);
        TimeSlot slot = timeSlotService.findSlotEntityOrThrow(slotId, calendar.getId());

        if (slot.isBusy()) {
            throw new BusinessRuleException("Cannot book a meeting on a busy slot");
        }

        if (slot.hasMeeting()) {
            throw new BusinessRuleException("This slot already has a meeting booked");
        }

        Set<User> participants = resolveParticipants(request.getParticipantIds());

        Meeting meeting = Meeting.builder()
                .timeSlot(slot)
                .title(request.getTitle())
                .description(request.getDescription())
                .organizer(organizer)
                .participants(participants)
                .build();

        // Mark the slot as busy when a meeting is booked
        slot.setStatus(SlotStatus.BUSY);
        slot.setMeeting(meeting);

        meeting = meetingRepository.save(meeting);
        log.info("Created meeting {} on slot {} for user {}", meeting.getId(), slotId, userId);
        return toResponse(meeting);
    }

    @Transactional(readOnly = true)
    public MeetingDto.Response getMeeting(Long userId, Long meetingId) {
        Meeting meeting = findMeetingOrThrow(meetingId, userId);
        return toResponse(meeting);
    }

    @Transactional(readOnly = true)
    public Page<MeetingDto.Response> listMeetings(Long userId, Pageable pageable) {
        userService.findUserOrThrow(userId);
        return meetingRepository.findByOrganizerId(userId, pageable).map(this::toResponse);
    }

    @Transactional
    public MeetingDto.Response updateMeeting(Long userId, Long meetingId, MeetingDto.UpdateRequest request) {
        Meeting meeting = findMeetingOrThrow(meetingId, userId);

        meeting.setTitle(request.getTitle());
        meeting.setDescription(request.getDescription());

        if (request.getParticipantIds() != null) {
            Set<User> participants = resolveParticipants(request.getParticipantIds());
            meeting.setParticipants(participants);
        }

        meeting = meetingRepository.save(meeting);
        log.info("Updated meeting {} for user {}", meetingId, userId);
        return toResponse(meeting);
    }

    @Transactional
    public void cancelMeeting(Long userId, Long meetingId) {
        Meeting meeting = findMeetingOrThrow(meetingId, userId);

        // Free up the time slot
        TimeSlot slot = meeting.getTimeSlot();
        slot.setStatus(SlotStatus.FREE);
        slot.setMeeting(null);

        meetingRepository.delete(meeting);
        log.info("Cancelled meeting {} for user {}", meetingId, userId);
    }

    private Meeting findMeetingOrThrow(Long meetingId, Long userId) {
        return meetingRepository.findByIdAndOrganizerId(meetingId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Meeting", meetingId));
    }

    private Set<User> resolveParticipants(List<Long> participantIds) {
        if (participantIds == null || participantIds.isEmpty()) {
            return new HashSet<>();
        }

        List<User> users = userRepository.findAllById(participantIds);
        if (users.size() != participantIds.size()) {
            Set<Long> foundIds = users.stream().map(User::getId).collect(Collectors.toSet());
            List<Long> missingIds = participantIds.stream()
                    .filter(id -> !foundIds.contains(id))
                    .toList();
            throw new ResourceNotFoundException("Users not found with ids: " + missingIds);
        }

        return new HashSet<>(users);
    }

    private MeetingDto.Response toResponse(Meeting meeting) {
        TimeSlot slot = meeting.getTimeSlot();

        Set<UserDto.Response> participantDtos = meeting.getParticipants().stream()
                .map(p -> UserDto.Response.builder()
                        .id(p.getId())
                        .name(p.getName())
                        .email(p.getEmail())
                        .build())
                .collect(Collectors.toSet());

        return MeetingDto.Response.builder()
                .id(meeting.getId())
                .title(meeting.getTitle())
                .description(meeting.getDescription())
                .startTime(slot.getStartTime())
                .endTime(slot.getEndTime())
                .durationMinutes(Duration.between(slot.getStartTime(), slot.getEndTime()).toMinutes())
                .organizer(UserDto.Response.builder()
                        .id(meeting.getOrganizer().getId())
                        .name(meeting.getOrganizer().getName())
                        .email(meeting.getOrganizer().getEmail())
                        .build())
                .participants(participantDtos)
                .timeSlotId(slot.getId())
                .createdAt(meeting.getCreatedAt() != null ? meeting.getCreatedAt().toString() : null)
                .build();
    }
}
