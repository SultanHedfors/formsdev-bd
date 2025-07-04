package com.example.demo.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ActivityAssignmentLogNotFoundException extends RuntimeException {

    public ActivityAssignmentLogNotFoundException(Integer activityId) {
        super(String.format("Nie znaleziono rekordu przypisania dla zajęcia: %s", activityId));

    }
}

