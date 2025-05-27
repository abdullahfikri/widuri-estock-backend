package dev.mfikri.widuriestock.controller;

import dev.mfikri.widuriestock.model.WebResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

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
//        log.info(exception.getClass().getName());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(WebResponse.
                        <String>builder()
                        .errors("Username or password wrong")
                        .build());

    }


}
