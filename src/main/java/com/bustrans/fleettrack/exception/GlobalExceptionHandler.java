package com.bustrans.fleettrack.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<Map<String, String>> handleDataAccess(DataAccessException ex) {
        log.error("Database error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("message", "Something went wrong, please try again."));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "";
        String lower = message.toLowerCase();

        HttpStatus status;
        if (lower.contains("not found")) {
            status = HttpStatus.NOT_FOUND;                 // 404
        } else if (lower.contains("already exists") || lower.contains("already in use")) {
            status = HttpStatus.CONFLICT;                  // 409
        } else if (lower.contains("not available") || lower.contains("already cancelled")) {
            status = HttpStatus.BAD_REQUEST;               // 400
        } else {
            status = HttpStatus.BAD_REQUEST;               // 400 (default)
        }

        return ResponseEntity.status(status).body(Map.of("error", message));
    }
}
