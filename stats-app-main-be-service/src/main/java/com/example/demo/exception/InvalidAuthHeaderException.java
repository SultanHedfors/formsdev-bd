package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class InvalidAuthHeaderException extends RuntimeException {
    public InvalidAuthHeaderException() {
        super("JWT in the authentication header is null");
    }
}
