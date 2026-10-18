package com.edu.platform.security;

import com.edu.platform.common.BizException;

/**
 * Holds the authenticated caller for the duration of one request.
 */
public final class UserContext {

    private static final ThreadLocal<AuthenticatedUser> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(AuthenticatedUser user) {
        HOLDER.set(user);
    }

    public static AuthenticatedUser get() {
        return HOLDER.get();
    }

    public static void clear() {
        HOLDER.remove();
    }

    /** Returns the caller or throws 401 when the request is anonymous. */
    public static AuthenticatedUser require() {
        AuthenticatedUser user = HOLDER.get();
        if (user == null) {
            throw BizException.unauthorized("Please sign in first");
        }
        return user;
    }

    public static Long requireUserId() {
        return require().userId();
    }

    /** Returns the caller and additionally enforces the ADMIN role. */
    public static AuthenticatedUser requireAdmin() {
        AuthenticatedUser user = require();
        if (!user.isAdmin()) {
            throw BizException.forbidden("Administrator privileges are required");
        }
        return user;
    }
}
