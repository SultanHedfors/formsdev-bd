package com.example.demo.exception;

public class ActivityAssignmentLogNotFoundException extends RuntimeException {

    public ActivityAssignmentLogNotFoundException(Integer activityId) {
        super("Nie znaleziono rekordu przypisania dla zajęcia: " + activityId);
    }
}

