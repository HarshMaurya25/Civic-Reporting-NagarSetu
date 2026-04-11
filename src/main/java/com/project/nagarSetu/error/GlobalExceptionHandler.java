package com.project.nagarSetu.error;

import com.project.nagarSetu.error.exception.AccessDeniedUserException;
import com.project.nagarSetu.error.exception.IssueNotFoundException;
import com.project.nagarSetu.error.exception.VerificationCodeExpiredException;
import io.jsonwebtoken.JwtException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.security.sasl.AuthenticationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@ControllerAdvice
public class GlobalExceptionHandler {


    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<ApiError>> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        List<ApiError> errors = new ArrayList<>();

        exception.getBindingResult().getFieldErrors().forEach(error -> {
            errors.add(ApiError.builder()
                    .keyError(error.getField())
                    .valueError(error.getDefaultMessage())
                    .timeStamp(LocalDateTime.now())
                    .build());
        });

        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<List<ApiError>> handleBadCredentialsException(BadCredentialsException exception) {
        return buildError(
                exception.getMessage(),
                "Bad Credential",
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<List<ApiError>> handleDisabledException(DisabledException exception) {
        return buildError(
                exception.getMessage(),
                "Disable User",
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<List<ApiError>> handleAuthenticationException(AuthenticationException exception) {
        return buildError(
                exception.getMessage(),
                "Authentication Failed",
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<List<ApiError>> handleAuthenticationCredentialsNotFoundException(
            AuthenticationCredentialsNotFoundException exception) {
        return buildError(
                exception.getMessage(),
                "Authentication Credentials Not Found",
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<List<ApiError>> handleAccessDenied(AccessDeniedException exception) {
        return buildError(
                exception.getMessage(),
                "Access Denied",
                HttpStatus.FORBIDDEN
        );
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<List<ApiError>> handleJwtException(JwtException exception) {
        return buildError(
                exception.getMessage(),
                "Invalid JWT Token",
                HttpStatus.UNAUTHORIZED
        );
    }

    @ExceptionHandler(VerificationCodeExpiredException.class)
    public ResponseEntity<List<ApiError>> handleVerificationCodeExpiredException(
            VerificationCodeExpiredException exception) {
        return buildError(
                exception.getMessage(),
                "Verification Code Expired",
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(AccessDeniedUserException.class)
    public ResponseEntity<List<ApiError>> handleAccessDeniedUserException(
            AccessDeniedUserException exception) {
        return buildError(
                exception.getMessage(),
                "Access Denied User Exception",
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(IssueNotFoundException.class)
    public ResponseEntity<List<ApiError>> handleIssueNotFoundException(
            IssueNotFoundException exception) {
        return buildError(
                exception.getMessage(),
                "Issue Not Found",
                HttpStatus.NOT_FOUND
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<List<ApiError>> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        Throwable rootCause = exception.getRootCause();
        String message = rootCause != null ? rootCause.getMessage() : exception.getMessage();

        if (message.contains("email")) {
            message = "email";
        } else if (message.contains("username")) {
            message = "username";
        }

        return buildError(
                message,
                "Already Exists",
                HttpStatus.BAD_REQUEST
        );
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<List<ApiError>> handleGenericException(RuntimeException exception) {
        exception.printStackTrace();
        return buildError(
                exception.getMessage(),
                "Error Occured",
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }


    private ResponseEntity<List<ApiError>> buildError(
            String keyError,
            String valueError,
            HttpStatus status) {

        List<ApiError> error = List.of(
                ApiError.builder()
                        .keyError(keyError)
                        .valueError(valueError)
                        .timeStamp(LocalDateTime.now())
                        .build()
        );

        return new ResponseEntity<>(error, status);
    }
}
