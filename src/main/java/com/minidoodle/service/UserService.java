package com.minidoodle.service;

import com.minidoodle.domain.Calendar;
import com.minidoodle.domain.User;
import com.minidoodle.dto.UserDto;
import com.minidoodle.exception.BusinessRuleException;
import com.minidoodle.exception.ResourceNotFoundException;
import com.minidoodle.repository.CalendarRepository;
import com.minidoodle.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final CalendarRepository calendarRepository;

    @Transactional
    public UserDto.Response createUser(UserDto.CreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessRuleException("User with email " + request.getEmail() + " already exists");
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .build();
        user = userRepository.save(user);

        // Auto-create a calendar for the user (domain concept)
        Calendar calendar = Calendar.builder()
                .user(user)
                .timezone("UTC")
                .build();
        calendarRepository.save(calendar);
        user.setCalendar(calendar);

        log.info("Created user {} with calendar", user.getId());
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public UserDto.Response getUser(Long userId) {
        User user = findUserOrThrow(userId);
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public Page<UserDto.Response> listUsers(Pageable pageable) {
        return userRepository.findAll(pageable).map(this::toResponse);
    }

    public User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private UserDto.Response toResponse(User user) {
        return UserDto.Response.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toString() : null)
                .build();
    }
}
