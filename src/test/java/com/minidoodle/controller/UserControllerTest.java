package com.minidoodle.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.minidoodle.dto.UserDto;
import com.minidoodle.exception.BusinessRuleException;
import com.minidoodle.exception.GlobalExceptionHandler;
import com.minidoodle.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @Test
    @DisplayName("POST /api/v1/users - should create user")
    void shouldCreateUser() throws Exception {
        var request = UserDto.CreateRequest.builder()
                .name("Alice").email("alice@example.com").build();
        var response = UserDto.Response.builder()
                .id(1L).name("Alice").email("alice@example.com").build();

        when(userService.createUser(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    @DisplayName("POST /api/v1/users - should return 400 for invalid input")
    void shouldReturn400ForInvalidInput() throws Exception {
        var request = UserDto.CreateRequest.builder()
                .name("").email("not-an-email").build();

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/users - should return 409 for duplicate email")
    void shouldReturn409ForDuplicateEmail() throws Exception {
        var request = UserDto.CreateRequest.builder()
                .name("Alice").email("alice@example.com").build();

        when(userService.createUser(any())).thenThrow(new BusinessRuleException("already exists"));

        mockMvc.perform(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("GET /api/v1/users/{id} - should return user")
    void shouldGetUser() throws Exception {
        var response = UserDto.Response.builder()
                .id(1L).name("Alice").email("alice@example.com").build();

        when(userService.getUser(1L)).thenReturn(response);

        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Alice"));
    }
}
