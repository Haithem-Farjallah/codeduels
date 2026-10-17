package com.codeduels.common.exception;

import com.codeduels.common.api.StandardResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
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

    @ExceptionHandler(UnauthorisedException.class)
    public ResponseEntity<StandardResponse<Void>> handleException(UnauthorisedException exception){
        return createResponse(HttpStatus.UNAUTHORIZED,exception.getMessage());
    }
    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<StandardResponse<Void>> handleException(TooManyRequestsException exception){
        return createResponse(HttpStatus.TOO_MANY_REQUESTS,exception.getMessage());
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<StandardResponse<Void>> handleException(BadRequestException exception){
        return createResponse(HttpStatus.BAD_REQUEST,exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<StandardResponse<Void>> handleException(MethodArgumentNotValidException exception){
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Invalid request");
        return createResponse(HttpStatus.BAD_REQUEST,message);
    }

}
