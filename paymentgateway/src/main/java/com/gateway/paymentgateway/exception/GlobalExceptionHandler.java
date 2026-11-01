package com.gateway.paymentgateway.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail invalidBody(MethodArgumentNotValidException exception,
                              HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        problem.setTitle("Validation failed");
        problem.setDetail("One or more request fields are invalid");
        problem.setProperty("code", "VALIDATION_FAILED");
        problem.setProperty("errors", exception.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() == null
                                ? "Invalid value"
                                : error.getDefaultMessage(),
                        (first, ignored) -> first
                )));
        addMetadata(problem, request);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail invalidParameter(ConstraintViolationException exception,
                                   HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED",
                "Validation failed", "One or more request parameters are invalid",
                request);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingPathVariableException.class,
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            HttpRequestMethodNotSupportedException.class,
            IllegalArgumentException.class
    })
    ProblemDetail malformedRequest(Exception exception, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                "Invalid request", safeMessage(exception, "The request is invalid"),
                request);
    }

    @ExceptionHandler(ApiException.class)
    ProblemDetail apiError(ApiException exception, HttpServletRequest request) {
        return problem(exception.getStatus(), exception.getCode(),
                titleFor(exception.getStatus()), exception.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    ProblemDetail missingAuthentication(AuthenticationCredentialsNotFoundException exception,
                                       HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_REQUIRED",
                "Authentication required", "A valid Bearer token is required", request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ProblemDetail authenticationError(AuthenticationException exception,
                                      HttpServletRequest request) {
        return problem(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED",
                "Authentication failed", "Invalid or expired authentication credentials", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ProblemDetail accessDenied(AccessDeniedException exception, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "FORBIDDEN",
                "Access denied", "You do not have permission to access this resource", request);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail dataIntegrity(DataIntegrityViolationException exception,
                                HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "DATA_CONFLICT",
                "Data conflict", "The request conflicts with existing data", request);
    }

    @ExceptionHandler(RuntimeException.class)
    ProblemDetail legacyApplicationError(RuntimeException exception,
                                         HttpServletRequest request) {
        String message = exception.getMessage() == null
                ? "The request could not be completed"
                : exception.getMessage();
        String normalized = message.toLowerCase();

        if (normalized.contains("not found")) {
            return problem(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                    "Resource not found", message, request);
        }
        if (normalized.contains("invalid credentials")
                || normalized.contains("invalid email or password")) {
            return problem(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS",
                    "Authentication failed", "Invalid email or password", request);
        }
        if (normalized.contains("unauthorized")) {
            return problem(HttpStatus.FORBIDDEN, "FORBIDDEN",
                    "Access denied", message, request);
        }
        if (normalized.contains("not approved")
                || normalized.contains("not submitted")
                || normalized.contains("not allowed")
                || normalized.contains("already approved")
                || normalized.contains("already used")
                || normalized.contains("not in requested state")
                || normalized.contains("refund not requested")) {
            return problem(HttpStatus.CONFLICT, "REQUEST_CONFLICT",
                    "Request cannot be completed", message, request);
        }
        if (normalized.contains("expired")) {
            return problem(HttpStatus.GONE, "TOKEN_EXPIRED",
                    "Token expired", message, request);
        }
        if (normalized.contains("required")
                || normalized.contains("cannot be empty")
                || normalized.contains("invalid")) {
            return problem(HttpStatus.BAD_REQUEST, "INVALID_REQUEST",
                    "Invalid request", message, request);
        }
        String requestId = UUID.randomUUID().toString();
        log.error("Unhandled runtime exception requestId={} method={} path={}",
                requestId, request.getMethod(), request.getRequestURI(), exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Request could not be completed",
                "An unexpected error occurred. Use requestId " + requestId
                        + " when contacting support.",
                request);
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail unexpected(Exception exception, HttpServletRequest request) {
        String requestId = UUID.randomUUID().toString();
        log.error("Unhandled exception requestId={} method={} path={}",
                requestId, request.getMethod(), request.getRequestURI(), exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Request could not be completed",
                "An unexpected error occurred. Use requestId " + requestId
                        + " when contacting support.", request);
    }

    private ProblemDetail problem(HttpStatus status, String code, String title,
                                  String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatus(status);
        problem.setTitle(title);
        problem.setDetail(detail);
        problem.setProperty("code", code);
        addMetadata(problem, request);
        return problem;
    }

    private void addMetadata(ProblemDetail problem, HttpServletRequest request) {
        problem.setProperty("path", request.getRequestURI());
        problem.setProperty("timestamp", OffsetDateTime.now());
    }

    private String safeMessage(Exception exception, String fallback) {
        return exception.getMessage() == null || exception.getMessage().isBlank()
                ? fallback
                : exception.getMessage();
    }

    private String titleFor(HttpStatus status) {
        return switch (status) {
            case BAD_REQUEST -> "Invalid request";
            case UNAUTHORIZED -> "Authentication failed";
            case FORBIDDEN -> "Access denied";
            case NOT_FOUND -> "Resource not found";
            case CONFLICT -> "Request conflict";
            case SERVICE_UNAVAILABLE -> "Service unavailable";
            default -> "Request could not be completed";
        };
    }
}
