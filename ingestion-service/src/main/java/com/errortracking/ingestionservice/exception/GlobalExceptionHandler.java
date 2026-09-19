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
}