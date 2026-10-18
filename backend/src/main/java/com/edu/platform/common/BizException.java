package com.edu.platform.common;

import lombok.Getter;

/**
 * Business exception. The {@code code} is mapped to the HTTP status and to the
 * {@code code} field of {@link Result}.
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(String message) {
        this(Result.CODE_BAD_REQUEST, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public static BizException notFound(String message) {
        return new BizException(Result.CODE_NOT_FOUND, message);
    }

    public static BizException unauthorized(String message) {
        return new BizException(Result.CODE_UNAUTHORIZED, message);
    }

    public static BizException forbidden(String message) {
        return new BizException(Result.CODE_FORBIDDEN, message);
    }
}
