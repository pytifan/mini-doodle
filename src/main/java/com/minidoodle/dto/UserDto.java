package com.minidoodle.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

public class UserDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "Request to create a new user")
    public static class CreateRequest {

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        @Schema(description = "User's display name", example = "Alice Johnson")
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        @Size(max = 255, message = "Email must not exceed 255 characters")
        @Schema(description = "User's email address", example = "alice@example.com")
        private String email;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "User response")
    public static class Response {

        @Schema(description = "User ID", example = "1")
        private Long id;

        @Schema(description = "User's display name", example = "Alice Johnson")
        private String name;

        @Schema(description = "User's email address", example = "alice@example.com")
        private String email;

        @Schema(description = "Timestamp of creation")
        private String createdAt;
    }
}
