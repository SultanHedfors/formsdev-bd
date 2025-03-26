package com.example.demo.controller;

import com.example.demo.exception.ActivityAssignmentLogNotFoundException;
import com.example.demo.exception.CurrentUserNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ActivityAssignmentLogNotFoundException.class)
    public ResponseEntity<String> handleAssignmentNotFound(ActivityAssignmentLogNotFoundException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }
    @ExceptionHandler(CurrentUserNotFoundException.class)
    public ResponseEntity<String> handleAssignmentNotFound(CurrentUserNotFoundException ex) {
        return new ResponseEntity<>(ex.getMessage(), HttpStatus.NOT_FOUND);
    }
}