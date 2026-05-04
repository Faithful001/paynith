package com.king.paysim.common.exception_handlers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.king.paysim.domain")
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseStatusException handleValidation(
            MethodArgumentNotValidException ex) {

        List<Map<String, String>> errors =
                ex.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(err -> {
                            assert err.getDefaultMessage() != null;
                            return Map.of(
                                    "field", err.getField(),
                                    "message", err.getDefaultMessage()
                            );
                        })
                        .toList();

        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                errors.toString()
        );
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseStatusException handleStatus(ResponseStatusException ex) {
        return ex;
    }

    @ExceptionHandler(Exception.class)
    public ResponseStatusException handleGeneric(Exception ex) {
        return new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal server error"
        );
    }
}
