package com.minidoodle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minidoodle.domain.SlotStatus;
import com.minidoodle.dto.AvailabilityDto;
import com.minidoodle.dto.TimeSlotDto;
import com.minidoodle.service.TimeSlotService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(TimeSlotController.class)
class TimeSlotControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TimeSlotService timeSlotService;

    private final LocalDateTime START = LocalDateTime.of(2025, 3, 1, 9, 0);
    private final LocalDateTime END = LocalDateTime.of(2025, 3, 1, 10, 0);

    @Test
    @DisplayName("POST /api/v1/users/{userId}/slots - should create slot")
    void shouldCreateSlot() throws Exception {
        var request = TimeSlotDto.CreateRequest.builder()
                .startTime(START).endTime(END).build();
        var response = TimeSlotDto.Response.builder()
                .id(1L).startTime(START).endTime(END)
                .durationMinutes(60).status(SlotStatus.FREE).hasMeeting(false).build();

        when(timeSlotService.createSlot(eq(1L), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/users/1/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.durationMinutes").value(60))
                .andExpect(jsonPath("$.status").value("FREE"));
    }

    @Test
    @DisplayName("PATCH /api/v1/users/{userId}/slots/{slotId}/status - should change status")
    void shouldChangeStatus() throws Exception {
        var request = new TimeSlotDto.StatusRequest(SlotStatus.BUSY);
        var response = TimeSlotDto.Response.builder()
                .id(1L).startTime(START).endTime(END)
                .durationMinutes(60).status(SlotStatus.BUSY).build();

        when(timeSlotService.updateSlotStatus(eq(1L), eq(1L), any())).thenReturn(response);

        mockMvc.perform(patch("/api/v1/users/1/slots/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BUSY"));
    }

    @Test
    @DisplayName("DELETE /api/v1/users/{userId}/slots/{slotId} - should delete slot")
    void shouldDeleteSlot() throws Exception {
        mockMvc.perform(delete("/api/v1/users/1/slots/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/availability - should return availability")
    void shouldReturnAvailability() throws Exception {
        var availability = AvailabilityDto.builder()
                .userId(1L).userName("Alice")
                .from(START).to(END)
                .totalSlots(2).freeSlots(1).busySlots(1)
                .slots(List.of(
                        AvailabilityDto.SlotSummary.builder()
                                .slotId(1L).startTime(START).endTime(END)
                                .durationMinutes(60).status(SlotStatus.FREE).build()
                ))
                .build();

        when(timeSlotService.getAvailability(eq(1L), any(), any())).thenReturn(availability);

        mockMvc.perform(get("/api/v1/users/1/availability")
                        .param("from", "2025-03-01T09:00:00")
                        .param("to", "2025-03-01T10:00:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSlots").value(2))
                .andExpect(jsonPath("$.freeSlots").value(1))
                .andExpect(jsonPath("$.busySlots").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/users/{userId}/availability - should require params")
    void shouldRequireAvailabilityParams() throws Exception {
        mockMvc.perform(get("/api/v1/users/1/availability"))
                .andExpect(status().isBadRequest());
    }
}
