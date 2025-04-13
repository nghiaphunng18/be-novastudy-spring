package com.aims.exception;

import com.aims.enums.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private ErrorResponse createErrorResponse(ErrorCode errorCode, String message, String path){
        return ErrorResponse.builder()
                .status(errorCode.getHttpStatus().value())
                .message(message)
                .path(path)
                .errorCode(errorCode.getErrorCode())
                .timestamp(LocalDateTime.now())
                .build();
    }

    //custom exception
    @ExceptionHandler(AppException.Exception.class)
    public ResponseEntity<ErrorResponse> handleBaseException(AppException.Exception exception, HttpServletRequest request) {
        ErrorCode code = exception.getErrorCode();
        ErrorResponse response = createErrorResponse(code, exception.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(response, code.getHttpStatus());
    }

    // MethodArgumentNotValidException: blank field LoginRequest
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException exception, HttpServletRequest request) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .collect(Collectors.joining(", "));

        ErrorResponse errorResponse = createErrorResponse(ErrorCode.BAD_REQUEST, message, request.getRequestURI());
        return new ResponseEntity<>(errorResponse, ErrorCode.BAD_REQUEST.getHttpStatus());
    }

    // Authentication exception
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(org.springframework.security.core.AuthenticationException exception, HttpServletRequest request) {
        ErrorResponse response = createErrorResponse(ErrorCode.INVALID_CREDENTIALS, ErrorCode.INVALID_CREDENTIALS.getMessage(), request.getRequestURI());
        return new ResponseEntity<>(response, ErrorCode.INVALID_CREDENTIALS.getHttpStatus());
    }

    //handler all exception
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllError(Exception exception, HttpServletRequest request) {
        logger.error("Unexpected error occurred: ", exception);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        ErrorResponse errorResponse = createErrorResponse(errorCode, exception.getMessage(), request.getRequestURI());

        return new ResponseEntity<>(errorResponse, errorCode.getHttpStatus());
    }
}
