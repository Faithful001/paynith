package com.king.paysim.common.exception_handlers;

import com.king.paysim.common.responses.Response;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.king.paysim.domain")
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Response<?>> handleValidation(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(err -> Map.of(
                        "field", err.getField(),
                        "message", err.getDefaultMessage() != null ? err.getDefaultMessage() : "Invalid value"
                ))
                .toList();

        return new ResponseEntity<>(
                Response.error(errors.toString(), ex.getStatusCode().value()),
                ex.getStatusCode()
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Response<?>> handleStatus(ResponseStatusException ex) {
        return new ResponseEntity<>(
                Response.error(ex.getReason(), ex.getStatusCode().value()),
                HttpStatus.valueOf(ex.getStatusCode().value())
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Response<?>> handleDataIntegrity(DataIntegrityViolationException ex) {
        return new ResponseEntity<>(
                Response.error("Data conflict: " + ex.getMostSpecificCause().getMessage(), HttpStatus.CONFLICT.value()),
                HttpStatus.CONFLICT
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response<?>> handleGeneric(Exception ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Internal Server Error";
        return new ResponseEntity<>(
                Response.error(message, HttpStatus.INTERNAL_SERVER_ERROR.value()),
                HttpStatus.INTERNAL_SERVER_ERROR
        );
    }
}