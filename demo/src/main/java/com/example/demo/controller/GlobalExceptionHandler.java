package com.example.demo.controller;

import com.example.demo.dto.ScheduleUploadResponseDto;
import com.example.demo.exception.ActivityAssignmentLogNotFoundException;
import com.example.demo.exception.CurrentUserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ActivityAssignmentLogNotFoundException.class)
    public ResponseEntity<String> handleAssignmentNotFound(ActivityAssignmentLogNotFoundException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(CurrentUserNotFoundException.class)
    public ResponseEntity<String> handleUserNotFound(CurrentUserNotFoundException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<String> handleAuthException() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Invalid credentials");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ScheduleUploadResponseDto> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new ScheduleUploadResponseDto(false, ex.getMessage()));
    }



    @ExceptionHandler(Exception.class)
    public ResponseEntity<ScheduleUploadResponseDto> handleGeneric(Exception ex) {
        return ResponseEntity.internalServerError().body(new ScheduleUploadResponseDto(false, "Server error: " + ex.getMessage()));
    }
}