package com.edu.platform.controller;

import com.edu.platform.common.Result;
import com.edu.platform.dto.AuthDtos;
import com.edu.platform.common.PageResult;
import com.edu.platform.security.UserContext;
import com.edu.platform.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Feature 5: administrator management of the accounts.
 */
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AuthService authService;

    @GetMapping
    public Result<PageResult<AuthDtos.UserVO>> list(@RequestParam(required = false) String keyword,
                                                    @RequestParam(defaultValue = "1") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        UserContext.requireAdmin();
        return Result.success(authService.listUsers(keyword, page, size));
    }

    @PatchMapping("/{userId}/status")
    public Result<AuthDtos.UserVO> toggleStatus(@PathVariable Long userId) {
        UserContext.requireAdmin();
        return Result.success("Account status updated", authService.toggleUserStatus(userId));
    }
}
