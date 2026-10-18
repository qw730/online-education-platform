package com.edu.platform.security;

import com.edu.platform.domain.enums.Role;

/**
 * The caller extracted from the JWT for the current request.
 */
public record AuthenticatedUser(Long userId, String username, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
