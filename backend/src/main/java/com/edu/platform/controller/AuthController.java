package com.edu.platform.controller;

import com.edu.platform.common.Result;
import com.edu.platform.dto.AuthDtos;
import com.edu.platform.security.UserContext;
import com.edu.platform.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Feature 1: sign up / sign in and the current user profile.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public Result<AuthDtos.UserVO> register(@Valid @RequestBody AuthDtos.RegisterRequest request) {
        return Result.success("Registration successful", authService.register(request));
    }

    @PostMapping("/login")
    public Result<AuthDtos.LoginResponse> login(@Valid @RequestBody AuthDtos.LoginRequest request) {
        return Result.success("Signed in", authService.login(request));
    }

    @GetMapping("/me")
    public Result<AuthDtos.UserVO> me() {
        return Result.success(authService.getProfile(UserContext.requireUserId()));
    }

    @PutMapping("/profile")
    public Result<AuthDtos.UserVO> updateProfile(@Valid @RequestBody AuthDtos.ProfileUpdateRequest request) {
        return Result.success("Profile updated", authService.updateProfile(UserContext.requireUserId(), request));
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody AuthDtos.PasswordChangeRequest request) {
        authService.changePassword(UserContext.requireUserId(), request);
        return Result.success("Password updated", null);
    }
}
