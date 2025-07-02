package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class ScheduleValidationException extends RuntimeException {

    public ScheduleValidationException(String validationErrors) {
        super("Schedule validation errors have occurred" + validationErrors);
    }
}
