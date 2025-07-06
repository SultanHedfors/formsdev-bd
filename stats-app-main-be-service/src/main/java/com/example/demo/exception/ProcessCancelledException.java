package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ProcessCancelledException extends RuntimeException {
    public ProcessCancelledException() {
        super("Schedule processing was cancelled by user.");
    }
}
