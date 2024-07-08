package com.example.supportticketingsystem.dto.exception;

public class NonUniqueResultException extends RuntimeException {
    public NonUniqueResultException(String message) {
        super(message);
    }

}