package dev.mfikri.widuriestock.controller;

import dev.mfikri.widuriestock.exception.JwtAuthenticationException;
import dev.mfikri.widuriestock.model.WebResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.AccessDeniedException;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class ErrorController {

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<WebResponse<String>> constraintViolationException (ConstraintViolationException exception) {
        log.warn("Constrain violation error: {}", exception.getMessage(), exception);

        return ResponseEntity.badRequest()
                .body(WebResponse.
                        <String>builder()
                        .errors(exception.getMessage())
                        .build());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<WebResponse<String>> apiException (ResponseStatusException exception) {
        log.warn("API Error with status {}: {}", exception.getStatusCode(), exception.getReason(), exception);
        return ResponseEntity
                .status(exception.getStatusCode())
                .body(WebResponse.
                        <String>builder()
                        .errors(exception.getReason())
                        .build());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<WebResponse<String>> badCredentialException(BadCredentialsException exception) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(WebResponse.
                        <String>builder()
                        .errors("Username or password wrong")
                        .build());
    }

    @ExceptionHandler(AuthenticationCredentialsNotFoundException.class)
    public ResponseEntity<WebResponse<String>> authenticationException(AuthenticationCredentialsNotFoundException exception) {
        log.warn("Authentication credentials not found: {}", exception.getMessage(), exception);

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(WebResponse
                        .<String>builder()
                        .errors("Authentication failed")
                        .build());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<WebResponse<String>> methodArgumentPathTypeMismatch(MethodArgumentTypeMismatchException exception) {
        log.warn("Method argument type mismatch for property '{}'. Required type: {}. Value: '{}'",
                exception.getPropertyName(),
                exception.getRequiredType(),
                exception.getValue(),
                exception);

        String userMessage = createUserMessageForTypeMismatch(exception.getPropertyName(), exception.getRequiredType(), exception.getValue());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(WebResponse
                        .<String>builder()
                        .errors(userMessage)
                        .build());
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<WebResponse<String>> handleBindingExceptions(BindException exception) {
        FieldError fieldError = exception.getBindingResult().getFieldError();
        String userMessage;
        if (fieldError != null && "typeMismatch".equals(fieldError.getCode())) {
            log.warn("Method argument type mismatch on object binding. Field: '{}', Rejected Value: '{}'",
                    fieldError.getField(),
                    fieldError.getRejectedValue(),
                    exception);
            userMessage = createUserMessageForTypeMismatch(fieldError.getField(), null, fieldError.getRejectedValue());

        } else if (fieldError != null) {
            userMessage = fieldError.getDefaultMessage();
            log.warn("Constraint violation: {}", userMessage, exception);
        } else {
            userMessage = "Invalid request data provided.";
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(WebResponse.<String>builder().errors(userMessage).build());
    }

    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<WebResponse<String>> jwtAuthenticationException(AuthenticationException exception) {
        log.warn("JWT authentication failed: {}", exception.getMessage(), exception);

        String message =  exception.getMessage();

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(WebResponse
                        .<String>builder()
                        .errors(message != null ? message: "Invalid Access Token")
                        .build());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<WebResponse<String>> forbiddenAccessException(AuthorizationDeniedException exception) {
        log.warn("Authorization denied: {}", exception.getMessage(), exception);

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(WebResponse
                        .<String>builder()
                        .errors("Forbidden Access")
                        .build());
    }

    // sql
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<WebResponse<String>> conflictLockingException(ObjectOptimisticLockingFailureException exception) {
        log.warn("Optimistic locking failure: {}", exception.getMessage(), exception);

        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(WebResponse.<String>builder()
                        .errors("Data was updated by another user. Please refresh and retry.")
                        .build());
    }

    private String createUserMessageForTypeMismatch(String propertyName, Class<?> requiredType, Object invalidValue) {

        if (requiredType != null && java.time.temporal.Temporal.class.isAssignableFrom(requiredType)) {
            return String.format("Invalid date format for property '%s'. Please use YYYY-MM-DD format.", propertyName);
        } else if (requiredType != null && Number.class.isAssignableFrom(requiredType)) {
            return String.format("Invalid number format for property '%s'. Value '%s' is not a valid number.", propertyName, invalidValue);
        } else {
            // A generic fallback for other types or when the required type isn't available (like from a FieldError)
            return String.format("Invalid format for property '%s'. Please check the data type.", propertyName);
        }
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<WebResponse<String>> handleDataIntegrityViolation(DataIntegrityViolationException exception) {
        String message = exception.getMostSpecificCause().getMessage().toLowerCase();

        log.warn("Data integrity violation: {}", message, exception);
        // Add "duplicate entry" to catch errors from MySQL
        if (message.contains("unique constraint") || message.contains("duplicate key") || message.contains("duplicate entry")){
            String userMessage;
            // Provide more specific feedback based on the constraint name
            if (message.contains("unique_supplier_name")) {
                userMessage = "Supplier name is already taken. Please use a different name.";
            } else if (message.contains("unique_email")) {
                userMessage = "Email is already registered. Please use a different email.";
            } else {
                // A good generic fallback for other unique constraints
                userMessage = "A record with one of the unique fields already exists.";
            }
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(WebResponse.<String>builder().errors(userMessage).build());
        }

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(WebResponse.<String>builder()
                        .errors("Data integrity violation: " + exception.getMostSpecificCause().getMessage())
                        .build());
    }

}
