package com.eneik.generated.controller;

import com.eneik.generated.dto.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.toList());

        ErrorResponse errorResponse = new ErrorResponse(
                "VALIDATION_FAILED",
                "Input validation failed",
                details
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex) {
        String message = ex.getMessage();
        HttpStatus status = HttpStatus.BAD_REQUEST;
        String code = "INVALID_REQUEST";

        if (message != null) {
            if (message.contains("not found")) {
                status = HttpStatus.NOT_FOUND;
                code = "RESOURCE_NOT_FOUND";
            } else if (message.contains("under 1 month old")) {
                status = HttpStatus.BAD_REQUEST;
                code = "INVALID_ACCOUNT_AGE";
            }
        }

        ErrorResponse errorResponse = new ErrorResponse(code, message);
        return ResponseEntity.status(status).body(errorResponse);
    }
}
