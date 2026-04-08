package com.example.bankcards.exception;

public class ApiException extends RuntimeException {

    public ApiException(String message) {
        super(message);
    }
}
