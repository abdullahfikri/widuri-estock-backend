package dev.mfikri.widuriestock.controller;

import dev.mfikri.widuriestock.exception.JwtAuthenticationException;
import dev.mfikri.widuriestock.model.WebResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.AuthenticationException;
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
        return ResponseEntity.badRequest()
                .body(WebResponse.
                        <String>builder()
                        .errors(exception.getMessage())
                        .build());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<WebResponse<String>> apiException (ResponseStatusException exception) {
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
        log.info(exception.getClass().getName());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(WebResponse
                        .<String>builder()
                        .errors("Authentication failed")
                        .build());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<WebResponse<String>> methodArgumentPathTypeMismatch(MethodArgumentTypeMismatchException exception) {
        log.info(exception.getClass().getName());
        log.info(exception.getPropertyName());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(WebResponse
                        .<String>builder()
                        .errors(exception.getPropertyName() + " type data is wrong.")
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<WebResponse<String>> methodArgumentTypeMismatch(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(","));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(WebResponse
                        .<String>builder()
                        .errors(message)
                        .build());
    }

    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<WebResponse<String>> jwtAuthenticationException(AuthenticationException exception) {
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
//        String message =  exception.getMessage();

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
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(WebResponse.<String>builder()
                        .errors("Data was updated by another user. Please refresh and retry.")
                        .build());
    }

}
