package com.minidoodle.service;

import com.minidoodle.domain.*;
import com.minidoodle.dto.TimeSlotDto;
import com.minidoodle.exception.BusinessRuleException;
import com.minidoodle.exception.ResourceNotFoundException;
import com.minidoodle.repository.CalendarRepository;
import com.minidoodle.repository.TimeSlotRepository;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimeSlotServiceTest {

    @Mock
    private TimeSlotRepository timeSlotRepository;

    @Mock
    private CalendarRepository calendarRepository;

    @Mock
    private UserService userService;

    @InjectMocks
    private TimeSlotService timeSlotService;

    private User testUser;
    private Calendar testCalendar;
    private TimeSlot testSlot;

    private final LocalDateTime START = LocalDateTime.of(2025, 3, 1, 9, 0);
    private final LocalDateTime END = LocalDateTime.of(2025, 3, 1, 10, 0);

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).name("Alice").email("alice@example.com").build();
        testCalendar = Calendar.builder().id(1L).user(testUser).build();
        testSlot = TimeSlot.builder()
                .id(1L)
                .calendar(testCalendar)
                .startTime(START)
                .endTime(END)
                .status(SlotStatus.FREE)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("createSlot")
    class CreateSlot {

        @Test
        @DisplayName("should create a free slot successfully")
        void shouldCreateSlot() {
            var request = TimeSlotDto.CreateRequest.builder()
                    .startTime(START).endTime(END).build();

            when(userService.findUserOrThrow(1L)).thenReturn(testUser);
            when(calendarRepository.findByUserId(1L)).thenReturn(Optional.of(testCalendar));
            when(timeSlotRepository.existsOverlappingSlot(eq(1L), any(), any())).thenReturn(false);
            when(timeSlotRepository.save(any(TimeSlot.class))).thenReturn(testSlot);

            var response = timeSlotService.createSlot(1L, request);

            assertThat(response.getStartTime()).isEqualTo(START);
            assertThat(response.getEndTime()).isEqualTo(END);
            assertThat(response.getStatus()).isEqualTo(SlotStatus.FREE);
            assertThat(response.getDurationMinutes()).isEqualTo(60);
        }

        @Test
        @DisplayName("should reject overlapping slots")
        void shouldRejectOverlappingSlots() {
            var request = TimeSlotDto.CreateRequest.builder()
                    .startTime(START).endTime(END).build();

            when(userService.findUserOrThrow(1L)).thenReturn(testUser);
            when(calendarRepository.findByUserId(1L)).thenReturn(Optional.of(testCalendar));
            when(timeSlotRepository.existsOverlappingSlot(eq(1L), any(), any())).thenReturn(true);

            assertThatThrownBy(() -> timeSlotService.createSlot(1L, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("overlaps");
        }

        @Test
        @DisplayName("should reject invalid time range")
        void shouldRejectInvalidTimeRange() {
            var request = TimeSlotDto.CreateRequest.builder()
                    .startTime(END).endTime(START).build();

            assertThatThrownBy(() -> timeSlotService.createSlot(1L, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("End time must be after start time");
        }
    }

    @Nested
    @DisplayName("updateSlotStatus")
    class UpdateSlotStatus {

        @Test
        @DisplayName("should change status to BUSY")
        void shouldChangeStatusToBusy() {
            var request = new TimeSlotDto.StatusRequest(SlotStatus.BUSY);

            when(userService.findUserOrThrow(1L)).thenReturn(testUser);
            when(calendarRepository.findByUserId(1L)).thenReturn(Optional.of(testCalendar));
            when(timeSlotRepository.findByIdAndCalendarId(1L, 1L)).thenReturn(Optional.of(testSlot));
            when(timeSlotRepository.save(any(TimeSlot.class))).thenReturn(testSlot);

            var response = timeSlotService.updateSlotStatus(1L, 1L, request);

            assertThat(response).isNotNull();
            verify(timeSlotRepository).save(testSlot);
        }

        @Test
        @DisplayName("should reject freeing a slot with meeting")
        void shouldRejectFreeingSlotWithMeeting() {
            Meeting meeting = Meeting.builder().id(1L).title("Test").build();
            testSlot.setMeeting(meeting);
            var request = new TimeSlotDto.StatusRequest(SlotStatus.FREE);

            when(userService.findUserOrThrow(1L)).thenReturn(testUser);
            when(calendarRepository.findByUserId(1L)).thenReturn(Optional.of(testCalendar));
            when(timeSlotRepository.findByIdAndCalendarId(1L, 1L)).thenReturn(Optional.of(testSlot));

            assertThatThrownBy(() -> timeSlotService.updateSlotStatus(1L, 1L, request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot mark a slot as FREE while it has a meeting");
        }
    }

    @Nested
    @DisplayName("deleteSlot")
    class DeleteSlot {

        @Test
        @DisplayName("should delete a free slot")
        void shouldDeleteFreeSlot() {
            when(userService.findUserOrThrow(1L)).thenReturn(testUser);
            when(calendarRepository.findByUserId(1L)).thenReturn(Optional.of(testCalendar));
            when(timeSlotRepository.findByIdAndCalendarId(1L, 1L)).thenReturn(Optional.of(testSlot));

            timeSlotService.deleteSlot(1L, 1L);

            verify(timeSlotRepository).delete(testSlot);
        }

        @Test
        @DisplayName("should reject deleting slot with meeting")
        void shouldRejectDeletingSlotWithMeeting() {
            testSlot.setMeeting(Meeting.builder().id(1L).title("Test").build());

            when(userService.findUserOrThrow(1L)).thenReturn(testUser);
            when(calendarRepository.findByUserId(1L)).thenReturn(Optional.of(testCalendar));
            when(timeSlotRepository.findByIdAndCalendarId(1L, 1L)).thenReturn(Optional.of(testSlot));

            assertThatThrownBy(() -> timeSlotService.deleteSlot(1L, 1L))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("Cannot delete");
        }
    }

    @Nested
    @DisplayName("getAvailability")
    class GetAvailability {

        @Test
        @DisplayName("should return aggregated availability")
        void shouldReturnAggregatedAvailability() {
            LocalDateTime from = LocalDateTime.of(2025, 3, 1, 0, 0);
            LocalDateTime to = LocalDateTime.of(2025, 3, 1, 23, 59);

            TimeSlot busySlot = TimeSlot.builder()
                    .id(2L).calendar(testCalendar).startTime(END)
                    .endTime(END.plusHours(1)).status(SlotStatus.BUSY).build();

            when(userService.findUserOrThrow(1L)).thenReturn(testUser);
            when(calendarRepository.findByUserId(1L)).thenReturn(Optional.of(testCalendar));
            when(timeSlotRepository.findAllByCalendarIdAndTimeRange(1L, from, to))
                    .thenReturn(List.of(testSlot, busySlot));

            var result = timeSlotService.getAvailability(1L, from, to);

            assertThat(result.getTotalSlots()).isEqualTo(2);
            assertThat(result.getFreeSlots()).isEqualTo(1);
            assertThat(result.getBusySlots()).isEqualTo(1);
            assertThat(result.getUserName()).isEqualTo("Alice");
        }
    }
}
