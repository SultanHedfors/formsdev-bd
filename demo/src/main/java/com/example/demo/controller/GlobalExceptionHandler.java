package com.example.demo.controller;

import com.example.demo.dto.UploadResponseDto;
import com.example.demo.exception.ActivityAssignmentLogNotFoundException;
import com.example.demo.exception.CurrentUserNotFoundException;
import com.example.demo.exception.FileOperationException;
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
    public ResponseEntity<String> handleAuthException(AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body("Invalid credentials");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<UploadResponseDto> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(new UploadResponseDto(false, ex.getMessage()));
    }

    @ExceptionHandler(FileOperationException.class)
    public ResponseEntity<UploadResponseDto> handleFileOperation(FileOperationException ex) {
        return ResponseEntity.internalServerError().body(new UploadResponseDto(false, ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<UploadResponseDto> handleGeneric(Exception ex) {
        return ResponseEntity.internalServerError().body(new UploadResponseDto(false, "Błąd serwera: " + ex.getMessage()));
    }
}