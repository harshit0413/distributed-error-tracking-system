package com.errortracking.ingestionservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgumentException(IllegalArgumentException ex) {

        // Error ko ek clean JSON format mein wrap kar rahe hain
        Map<String, String> errorResponse = Map.of(
                "error", "Unauthorized",
                "message", ex.getMessage() // Yeh wahi message h jo Service se pass hua tha
        );

        // HTTP 401 (UNAUTHORIZED) return kar rahe hain
        return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRateLimitException(RuntimeException ex) {

        // Agar error message mein "Rate limit" likha hai, toh 429 bhejenge
        if (ex.getMessage() != null && ex.getMessage().contains("Rate limit")) {
            Map<String, String> errorResponse = Map.of(
                    "error", "Too Many Requests",
                    "message", ex.getMessage()
            );
            return new ResponseEntity<>(errorResponse, HttpStatus.TOO_MANY_REQUESTS);
        }

        // Varna baaki kisi bhi unknown error ke liye generic 500 bhejenge
        Map<String, String> genericError = Map.of(
                "error", "Internal Server Error",
                "message", "An unexpected error occurred"
        );
        return new ResponseEntity<>(genericError, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}