package com.example.demoproj.exception;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TokenException extends RuntimeException {
    private final String property;
    private final String message;
}
