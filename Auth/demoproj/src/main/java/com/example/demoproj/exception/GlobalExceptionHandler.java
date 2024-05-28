package com.example.demoproj.exception;

import com.example.demoproj.dto.exception.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.lang.invoke.MethodHandles;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @ExceptionHandler(value = TokenException.class)
    protected ResponseEntity<ErrorResponse> handleUnauthorizedRequest(TokenException ex) {

        ErrorResponse response = ErrorResponse.builder()
                .message(ex.getMessage())
                .variable(ex.getProperty())
                .build();

        logger.error("Error: {}", response);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(value = UserNotFoundException.class)
    protected ResponseEntity<ErrorResponse> handleUnauthorizedRequest(UserNotFoundException ex) {

        ErrorResponse response = ErrorResponse.builder()
                .message(ex.getMessage())
                .variable(String.valueOf(ex.getUserId()))
                .build();

        logger.error("Error: {}", response);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }
}