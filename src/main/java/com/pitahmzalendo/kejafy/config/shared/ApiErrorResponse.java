package com.pitahmzalendo.kejafy.config.shared;


import java.time.Instant;
import java.util.Map;

/**
 * Same shape RestAuthenticationEntryPoint already writes by hand for 401s
 * (timestamp/status/error/message) — GlobalExceptionHandler is the other
 * half, covering everything that reaches a controller. fieldErrors is
 * only populated for validation failures (@Valid), one message per
 * invalid field.
 */
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors
) {
    public static ApiErrorResponse of(int status, String error, String message) {
        return new ApiErrorResponse(Instant.now(), status, error, message, null);
    }

    public static ApiErrorResponse ofValidation(int status, String error, String message,
                                                Map<String, String> fieldErrors) {
        return new ApiErrorResponse(Instant.now(), status, error, message, fieldErrors);
    }
}
