package com.errortracking.ingestionservice.controller;

import com.errortracking.ingestionservice.dto.ErrorPayload;
import com.errortracking.ingestionservice.service.IngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/errors")
public class ErrorController {

    private final IngestionService ingestionService;

    // Constructor Injection for strict Enterprise Standard
    public ErrorController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public ResponseEntity<Void> captureError(
            @RequestHeader("X-API-Key") String apiKey,
            @RequestBody ErrorPayload payload) {

        // Service ko call kar rahe hain (Agar API key galat hui, toh yahi se Exception udh jayega)
        ingestionService.processError(apiKey, payload);

        // Agar upar exception nahi aaya, matlab sab theek hai. 202 Accepted bhej do.
        return ResponseEntity.accepted().build();
    }
}