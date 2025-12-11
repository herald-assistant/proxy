package com.acme.herald.web.error;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Map<String,Object>> onConflict(ConflictException e) {
        return ResponseEntity.status(409).body(Map.of("error","conflict","message", e.getMessage()));
    }
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Map<String,Object>> onNotFound(NotFoundException e) {
        return ResponseEntity.status(404).body(Map.of("error","not_found","message", e.getMessage()));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> onAny(Exception e) {
        return ResponseEntity.status(500).body(Map.of("error","server_error","message", e.getMessage()));
    }
}
