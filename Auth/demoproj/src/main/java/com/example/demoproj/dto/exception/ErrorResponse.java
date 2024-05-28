package com.example.demoproj.dto.exception;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorResponse {

    private String message;

    private String variable;

    @Override
    public String toString() {
        return String.format("Error response(message=%s, variable=%s)", message, variable);
    }
}
