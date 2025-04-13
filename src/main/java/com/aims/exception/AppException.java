package com.aims.exception;

import com.aims.enums.ErrorCode;
import lombok.Getter;
import lombok.ToString;

public class AppException {
    @Getter
    @ToString
    public static class Exception extends RuntimeException {
        private final ErrorCode errorCode;

        public Exception(ErrorCode errorCode) {
            super(errorCode.getMessage());
            this.errorCode = errorCode;
        }

        public Exception(ErrorCode errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
    }

    //factory
    public static Exception resourceNotFound() {
        return new Exception(ErrorCode.RESOURCE_NOT_FOUND);
    }
    public static Exception resourceNotFound(String message) {
        return new Exception(ErrorCode.RESOURCE_NOT_FOUND, message);
    }

    public static Exception badRequest() {
        return new Exception(ErrorCode.BAD_REQUEST);
    }
    public static Exception badRequest(String message) {
        return new Exception(ErrorCode.BAD_REQUEST, message);
    }

    public static Exception unauthorized() {
        return new Exception(ErrorCode.UNAUTHORIZED);
    }
    public static Exception unauthorized(String message) {
        return new Exception(ErrorCode.UNAUTHORIZED, message);
    }

    public static Exception forbidden() {
        return new Exception(ErrorCode.FORBIDDEN);
    }
    public static Exception forbidden(String message) {
        return new Exception(ErrorCode.FORBIDDEN, message);
    }

    public static Exception conflict() {
        return new Exception(ErrorCode.CONFLICT);
    }
    public static Exception conflict(String message) {
        return new Exception(ErrorCode.CONFLICT, message);
    }

    public static Exception internalServerError() {
        return new Exception(ErrorCode.INTERNAL_SERVER_ERROR);
    }
    public static Exception internalServerError(String message) {
        return new Exception(ErrorCode.INTERNAL_SERVER_ERROR, message);
    }

    public static Exception invalidCredentials() {
        return new Exception(ErrorCode.INVALID_CREDENTIALS);
    }
    public static Exception invalidCredentials(String message) {
        return new Exception(ErrorCode.INVALID_CREDENTIALS, message);
    }
}
