package com.example.demo.exception;

public class CurrentUserNotFoundException extends RuntimeException  {

    public CurrentUserNotFoundException() {
        super("Nie znaleziono zalogowanego uzytkownika");
    }
}
