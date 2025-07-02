package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class CancelProcessingFailedException extends RuntimeException {
    public CancelProcessingFailedException() {
        super("Canceling schedule processing was unsuccessful");
    }
}
