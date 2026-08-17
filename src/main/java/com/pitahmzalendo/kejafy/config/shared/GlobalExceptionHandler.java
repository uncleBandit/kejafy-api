package com.pitahmzalendo.kejafy.config.shared;

import com.pitahmzalendo.kejafy.agent.exception.AgentApplicationException;
import com.pitahmzalendo.kejafy.agent.exception.AgentApplicationNotFoundException;
import com.pitahmzalendo.kejafy.chat.exception.ChatAccessException;
import com.pitahmzalendo.kejafy.chat.exception.ChatException;
import com.pitahmzalendo.kejafy.chat.exception.ChatNotFoundException;
import com.pitahmzalendo.kejafy.listing.exception.CommentAccessException;
import com.pitahmzalendo.kejafy.listing.exception.CommentException;
import com.pitahmzalendo.kejafy.listing.exception.CommentNotFoundException;
import com.pitahmzalendo.kejafy.listing.exception.ListingAccessException;
import com.pitahmzalendo.kejafy.listing.exception.ListingException;
import com.pitahmzalendo.kejafy.listing.exception.ListingNotFoundException;
import com.pitahmzalendo.kejafy.payment.exception.PaymentException;
import com.pitahmzalendo.kejafy.payment.exception.PaymentNotFoundException;
import com.pitahmzalendo.kejafy.payment.exception.PaymentProviderException;
import com.pitahmzalendo.kejafy.user.exception.AccountLinkingException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Single place that turns thrown exceptions into the JSON error shape
 * clients can rely on (see ApiErrorResponse) — same
 * timestamp/status/error/message fields RestAuthenticationEntryPoint
 * already writes by hand for 401s, so a client never has to branch on
 * "was this a 401 or something else" to find the message.
 *
 * Add a case here whenever a new domain exception is introduced, rather
 * than letting it fall through to Spring's default (which would return
 * a bare 500 for anything not explicitly mapped).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // --- 400s: request was understood but rejected by domain rules ---

    @ExceptionHandler({AgentApplicationException.class, ListingException.class,
            CommentException.class, ChatException.class, PaymentException.class})
    public ResponseEntity<ApiErrorResponse> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest()
                .body(ApiErrorResponse.of(400, "Bad Request", ex.getMessage()));
    }

    // --- 404: more specific than the 400 case above — Spring resolves to
    // whichever @ExceptionHandler is closest in the thrown exception's
    // hierarchy, so these subtypes win over handleBadRequest even though
    // it also matches their supertypes. ---

    @ExceptionHandler({ListingNotFoundException.class, AgentApplicationNotFoundException.class,
            CommentNotFoundException.class, ChatNotFoundException.class, PaymentNotFoundException.class})
    public ResponseEntity<ApiErrorResponse> handleNotFound(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    // --- 502: our request was fine, but the payment provider (Daraja/
    // Stripe) rejected it or couldn't be reached. ---

    @ExceptionHandler(PaymentProviderException.class)
    public ResponseEntity<ApiErrorResponse> handlePaymentProviderError(PaymentProviderException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(ApiErrorResponse.of(502, "Bad Gateway", ex.getMessage()));
    }

    @ExceptionHandler(AccountLinkingException.class)
    public ResponseEntity<ApiErrorResponse> handleAccountLinking(AccountLinkingException ex) {
        // 409: the request is well-formed, but conflicts with an existing
        // account/email — not a validation failure on the payload itself.
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    // --- 403: authenticated, but not allowed to touch this resource ---

    @ExceptionHandler({ListingAccessException.class, CommentAccessException.class,
            ChatAccessException.class, AccessDeniedException.class})
    public ResponseEntity<ApiErrorResponse> handleForbidden(RuntimeException ex) {
        String message = ex instanceof AccessDeniedException
                ? "You do not have permission to perform this action."
                : ex.getMessage();
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiErrorResponse.of(403, "Forbidden", message));
    }

    // --- 400: @Valid-annotated request bodies (CreateListingRequest, AgentApplicationRequest, etc) ---

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.put(fieldError.getField(),
                        fieldError.getDefaultMessage() != null
                                ? fieldError.getDefaultMessage()
                                : "invalid value"));

        return ResponseEntity.badRequest().body(ApiErrorResponse.ofValidation(
                400, "Validation Failed", "One or more fields failed validation.", fieldErrors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation ->
                fieldErrors.put(violation.getPropertyPath().toString(), violation.getMessage()));

        return ResponseEntity.badRequest().body(ApiErrorResponse.ofValidation(
                400, "Validation Failed", "One or more fields failed validation.", fieldErrors));
    }

    // --- fallback: anything not explicitly mapped above ---

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex) {
        log.error("Unhandled exception reached GlobalExceptionHandler", ex);
        return ResponseEntity.internalServerError().body(ApiErrorResponse.of(
                500, "Internal Server Error", "Something went wrong. Please try again."));
    }
}