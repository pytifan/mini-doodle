package com.minidoodle.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minidoodle.domain.SlotStatus;
import com.minidoodle.dto.MeetingDto;
import com.minidoodle.dto.TimeSlotDto;
import com.minidoodle.dto.UserDto;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MiniDoodleIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @Order(1)
    @DisplayName("Full workflow: create users -> create slots -> book meeting -> query availability -> cancel meeting")
    void fullWorkflow() throws Exception {
        // 1. Create organizer
        var createAlice = UserDto.CreateRequest.builder()
                .name("Alice").email("alice-integration@example.com").build();
        MvcResult aliceResult = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createAlice)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alice"))
                .andReturn();

        Long aliceId = objectMapper.readTree(aliceResult.getResponse().getContentAsString())
                .get("id").asLong();

        // 2. Create participant
        var createBob = UserDto.CreateRequest.builder()
                .name("Bob").email("bob-integration@example.com").build();
        MvcResult bobResult = mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createBob)))
                .andExpect(status().isCreated())
                .andReturn();

        Long bobId = objectMapper.readTree(bobResult.getResponse().getContentAsString())
                .get("id").asLong();

        // 3. Create time slots for Alice
        LocalDateTime slot1Start = LocalDateTime.of(2025, 6, 15, 9, 0);
        LocalDateTime slot1End = LocalDateTime.of(2025, 6, 15, 10, 0);
        LocalDateTime slot2Start = LocalDateTime.of(2025, 6, 15, 10, 0);
        LocalDateTime slot2End = LocalDateTime.of(2025, 6, 15, 11, 0);

        var slotReq1 = TimeSlotDto.CreateRequest.builder()
                .startTime(slot1Start).endTime(slot1End).build();
        MvcResult slot1Result = mockMvc.perform(post("/api/v1/users/" + aliceId + "/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slotReq1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FREE"))
                .andExpect(jsonPath("$.durationMinutes").value(60))
                .andReturn();

        Long slot1Id = objectMapper.readTree(slot1Result.getResponse().getContentAsString())
                .get("id").asLong();

        var slotReq2 = TimeSlotDto.CreateRequest.builder()
                .startTime(slot2Start).endTime(slot2End).build();
        MvcResult slot2Result = mockMvc.perform(post("/api/v1/users/" + aliceId + "/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(slotReq2)))
                .andExpect(status().isCreated())
                .andReturn();

        Long slot2Id = objectMapper.readTree(slot2Result.getResponse().getContentAsString())
                .get("id").asLong();

        // 4. Verify overlapping slot is rejected
        var overlappingSlot = TimeSlotDto.CreateRequest.builder()
                .startTime(slot1Start.plusMinutes(30))
                .endTime(slot1End.plusMinutes(30))
                .build();
        mockMvc.perform(post("/api/v1/users/" + aliceId + "/slots")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(overlappingSlot)))
                .andExpect(status().isConflict());

        // 5. List slots
        mockMvc.perform(get("/api/v1/users/" + aliceId + "/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));

        // 6. Book a meeting on slot 1
        var meetingReq = MeetingDto.CreateRequest.builder()
                .title("Sprint Planning")
                .description("Q2 sprint planning")
                .participantIds(List.of(bobId))
                .build();

        MvcResult meetingResult = mockMvc.perform(
                        post("/api/v1/users/" + aliceId + "/slots/" + slot1Id + "/meetings")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(meetingReq)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Sprint Planning"))
                .andExpect(jsonPath("$.organizer.name").value("Alice"))
                .andReturn();

        Long meetingId = objectMapper.readTree(meetingResult.getResponse().getContentAsString())
                .get("id").asLong();

        // 7. Verify slot 1 is now BUSY
        mockMvc.perform(get("/api/v1/users/" + aliceId + "/slots/" + slot1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BUSY"))
                .andExpect(jsonPath("$.hasMeeting").value(true));

        // 8. Try to book another meeting on the same slot - should fail
        mockMvc.perform(post("/api/v1/users/" + aliceId + "/slots/" + slot1Id + "/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(meetingReq)))
                .andExpect(status().isConflict());

        // 9. Query availability
        mockMvc.perform(get("/api/v1/users/" + aliceId + "/availability")
                        .param("from", "2025-06-15T00:00:00")
                        .param("to", "2025-06-15T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSlots").value(2))
                .andExpect(jsonPath("$.freeSlots").value(1))
                .andExpect(jsonPath("$.busySlots").value(1));

        // 10. Mark slot 2 as busy manually
        var busyReq = new TimeSlotDto.StatusRequest(SlotStatus.BUSY);
        mockMvc.perform(patch("/api/v1/users/" + aliceId + "/slots/" + slot2Id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(busyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BUSY"));

        // 11. Verify both slots now busy
        mockMvc.perform(get("/api/v1/users/" + aliceId + "/availability")
                        .param("from", "2025-06-15T00:00:00")
                        .param("to", "2025-06-15T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.busySlots").value(2))
                .andExpect(jsonPath("$.freeSlots").value(0));

        // 12. Cancel the meeting
        mockMvc.perform(delete("/api/v1/users/" + aliceId + "/meetings/" + meetingId))
                .andExpect(status().isNoContent());

        // 13. Verify slot 1 is free again
        mockMvc.perform(get("/api/v1/users/" + aliceId + "/slots/" + slot1Id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FREE"))
                .andExpect(jsonPath("$.hasMeeting").value(false));

        // 14. Delete slot 1 (now free, no meeting)
        mockMvc.perform(delete("/api/v1/users/" + aliceId + "/slots/" + slot1Id))
                .andExpect(status().isNoContent());

        // 15. Verify only 1 slot remains
        mockMvc.perform(get("/api/v1/users/" + aliceId + "/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @Order(2)
    @DisplayName("Should return 404 for non-existent user")
    void shouldReturn404ForNonExistentUser() throws Exception {
        mockMvc.perform(get("/api/v1/users/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(3)
    @DisplayName("Should reject duplicate email")
    void shouldRejectDuplicateEmail() throws Exception {
        var request = UserDto.CreateRequest.builder()
                .name("Duplicate").email("unique-test@example.com").build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
}
