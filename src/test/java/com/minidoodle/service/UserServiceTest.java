package com.minidoodle.service;

import com.minidoodle.domain.Calendar;
import com.minidoodle.domain.User;
import com.minidoodle.dto.UserDto;
import com.minidoodle.exception.BusinessRuleException;
import com.minidoodle.exception.ResourceNotFoundException;
import com.minidoodle.repository.CalendarRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CalendarRepository calendarRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .name("Alice")
                .email("alice@example.com")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Nested
    @DisplayName("createUser")
    class CreateUser {

        @Test
        @DisplayName("should create user and auto-provision calendar")
        void shouldCreateUserWithCalendar() {
            var request = UserDto.CreateRequest.builder()
                    .name("Alice")
                    .email("alice@example.com")
                    .build();

            when(userRepository.existsByEmail("alice@example.com")).thenReturn(false);
            when(userRepository.save(any(User.class))).thenReturn(testUser);
            when(calendarRepository.save(any(Calendar.class))).thenReturn(
                    Calendar.builder().id(1L).user(testUser).build());

            var response = userService.createUser(request);

            assertThat(response.getName()).isEqualTo("Alice");
            assertThat(response.getEmail()).isEqualTo("alice@example.com");
            verify(calendarRepository).save(any(Calendar.class));
        }

        @Test
        @DisplayName("should throw exception for duplicate email")
        void shouldRejectDuplicateEmail() {
            var request = UserDto.CreateRequest.builder()
                    .name("Alice")
                    .email("alice@example.com")
                    .build();

            when(userRepository.existsByEmail("alice@example.com")).thenReturn(true);

            assertThatThrownBy(() -> userService.createUser(request))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("already exists");
        }
    }

    @Nested
    @DisplayName("getUser")
    class GetUser {

        @Test
        @DisplayName("should return user when found")
        void shouldReturnUser() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

            var response = userService.getUser(1L);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getName()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("should throw exception when user not found")
        void shouldThrowWhenNotFound() {
            when(userRepository.findById(999L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> userService.getUser(999L))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }
}
