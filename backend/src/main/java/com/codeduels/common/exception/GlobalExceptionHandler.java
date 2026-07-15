package com.codeduels.common.exception;

import com.codeduels.common.api.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private ResponseEntity<StandardResponse<Void>> createResponse(HttpStatus status, String message){
        return ResponseEntity.status(status).body(StandardResponse.error(message));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<StandardResponse<Void>> handleException(InvalidCredentialsException exception){
        return createResponse(HttpStatus.UNAUTHORIZED,exception.getMessage());
    }

    @ExceptionHandler(RessourceNotFoundException.class)
    public ResponseEntity<StandardResponse<Void>> handleException(RessourceNotFoundException exception){
        return createResponse(HttpStatus.NOT_FOUND,exception.getMessage());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<StandardResponse<Void>> handleException(ConflictException exception){
        return createResponse(HttpStatus.CONFLICT,exception.getMessage());
    }

}
