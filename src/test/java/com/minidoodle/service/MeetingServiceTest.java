package com.minidoodle.service;

import com.minidoodle.domain.*;
import com.minidoodle.dto.MeetingDto;
import com.minidoodle.exception.BusinessRuleException;
import com.minidoodle.exception.ResourceNotFoundException;
import com.minidoodle.repository.MeetingRepository;
import com.minidoodle.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MeetingServiceTest {

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TimeSlotService timeSlotService;

    @Mock
    private UserService userService;

    @InjectMocks
    private MeetingService meetingService;

    private User organizer;
    private User participant;
    private Calendar calendar;
    private TimeSlot freeSlot;
    private TimeSlot busySlot;

    @BeforeEach
    void setUp() {
        organizer = User.builder().id(1L).name("Alice").email("alice@example.com").build();
        participant = User.builder().id(2L).name("Bob").email("bob@example.com").build();
        calendar = Calendar.builder().id(1L).user(organizer).build();

        freeSlot = TimeSlot.builder()
                .id(1L).calendar(calendar)
                .startTime(LocalDateTime.of(2025, 3, 1, 9, 0))
                .endTime(LocalDateTime.of(2025, 3, 1, 10, 0))
                .status(SlotStatus.FREE)
                .build();

        busySlot = TimeSlot.builder()
                .id(2L).calendar(calendar)
                .startTime(LocalDateTime.of(2025, 3, 1, 10, 0))
                .endTime(LocalDateTime.of(2025, 3, 1, 11, 0))
                .status(SlotStatus.BUSY)
                .build();
    }

    @Nested
    @DisplayName("createMeeting")
    class CreateMeeting {

        @Test
        @DisplayName("should create meeting from free slot")
        void shouldCreateMeetingFromFreeSlot() {
            var request = MeetingDto.CreateRequest.builder()
                    .title("Sprint Planning")
                    .description("Q1 planning")
                    .participantIds(List.of(2L))
                    .build();

            when(userService.findUserOrThrow(1L)).thenReturn(organizer);
            when(timeSlotService.findCalendarByUserId(1L)).thenReturn(calendar);
            when(timeSlotService.findSlotEntityOrThrow(1L, 1L)).thenReturn(freeSlot);
            when(userRepository.findAllById(List.of(2L))).thenReturn(List.of(participant));
            when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocation -> {
                Meeting m = invocation.getArgument(0);
                m.setId(1L);
                m.setCreatedAt(LocalDateTime.now());
                return m;
            });

            var response = meetingService.createMeeting(1L, 1L, request);

            assertThat(response.getTitle()).isEqualTo("Sprint Planning");
            assertThat(response.getDescription()).isEqualTo("Q1 planning");
            assertThat(freeSlot.getStatus()).isEqualTo(SlotStatus.BUSY);
        }

        @Test
        @DisplayName("should reject booking on busy slot")
        void shouldRejectBookingOnBusySlot() {
            var request = MeetingDto.CreateRequest.builder()
                    .title("Meeting").build();

            when(userService.findUserOrThrow(1L)).thenReturn(organizer);
            when(timeSlotService.findCalendarByUserId(1L)).thenReturn(calendar);
            when(timeSlotService.findSlotEntityOrThrow(2L, 1L)).thenReturn(busySlot);

            assertThatThrownBy(() -> meetingService.createMeeting(1L, 2L, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("busy slot");
        }

        @Test
        @DisplayName("should reject booking when slot already has meeting")
        void shouldRejectDoubleBooking() {
            freeSlot.setMeeting(Meeting.builder().id(99L).title("Existing").build());

            var request = MeetingDto.CreateRequest.builder()
                    .title("Meeting").build();

            when(userService.findUserOrThrow(1L)).thenReturn(organizer);
            when(timeSlotService.findCalendarByUserId(1L)).thenReturn(calendar);
            when(timeSlotService.findSlotEntityOrThrow(1L, 1L)).thenReturn(freeSlot);

            assertThatThrownBy(() -> meetingService.createMeeting(1L, 1L, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already has a meeting");
        }

        @Test
        @DisplayName("should reject non-existent participant")
        void shouldRejectNonExistentParticipant() {
            var request = MeetingDto.CreateRequest.builder()
                    .title("Meeting")
                    .participantIds(List.of(999L))
                    .build();

            when(userService.findUserOrThrow(1L)).thenReturn(organizer);
            when(timeSlotService.findCalendarByUserId(1L)).thenReturn(calendar);
            when(timeSlotService.findSlotEntityOrThrow(1L, 1L)).thenReturn(freeSlot);
            when(userRepository.findAllById(List.of(999L))).thenReturn(List.of());

            assertThatThrownBy(() -> meetingService.createMeeting(1L, 1L, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("999");
        }
    }

    @Nested
    @DisplayName("cancelMeeting")
    class CancelMeeting {

        @Test
        @DisplayName("should cancel meeting and free slot")
        void shouldCancelAndFreeSlot() {
            Meeting meeting = Meeting.builder()
                    .id(1L).title("Test").timeSlot(busySlot).organizer(organizer).build();
            busySlot.setMeeting(meeting);

            when(meetingRepository.findByIdAndOrganizerId(1L, 1L)).thenReturn(Optional.of(meeting));

            meetingService.cancelMeeting(1L, 1L);

            assertThat(busySlot.getStatus()).isEqualTo(SlotStatus.FREE);
            verify(meetingRepository).delete(meeting);
        }
    }
}
