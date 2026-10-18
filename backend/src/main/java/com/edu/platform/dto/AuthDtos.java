package com.edu.platform.dto;

import com.edu.platform.domain.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * Request / response payloads of the authentication endpoints.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "Username is required")
            @Size(min = 3, max = 30, message = "Username must be 3-30 characters")
            @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Username may only contain letters, digits and underscore")
            String username,

            @NotBlank(message = "Password is required")
            @Size(min = 6, max = 30, message = "Password must be 6-30 characters")
            String password,

            @Size(max = 50, message = "Name is too long")
            String realName,

            @Email(message = "Email format is invalid")
            String email) {
    }

    public record LoginRequest(
            @NotBlank(message = "Username is required") String username,
            @NotBlank(message = "Password is required") String password) {
    }

    public record LoginResponse(String token, String tokenType, long expiresIn, UserVO user) {
    }

    public record UserVO(
            Long id,
            String username,
            String realName,
            String email,
            Role role,
            String avatar,
            String bio,
            Integer status,
            LocalDateTime createdAt) {
    }

    public record ProfileUpdateRequest(
            @Size(max = 50) String realName,
            @Email(message = "Email format is invalid") String email,
            @Size(max = 255) String avatar,
            @Size(max = 255) String bio) {
    }

    public record PasswordChangeRequest(
            @NotBlank(message = "Current password is required") String oldPassword,
            @NotBlank(message = "New password is required")
            @Size(min = 6, max = 30, message = "New password must be 6-30 characters") String newPassword) {
    }
}
