package com.novastudy.enums;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;

@Getter
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public enum ErrorCode {
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "ERR_RESOURCE_NOT_FOUND", "Resource not found"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "ERR_INTERNAL_SERVER", "Unexpected error occurred"),
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "ERR_BAD_REQUEST", "Invalid request data"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "ERR_UNAUTHORIZED", "Authentication required"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "ERR_FORBIDDEN", "Access denied"),
    CONFLICT(HttpStatus.CONFLICT, "ERR_CONFLICT", "Data conflict occurred"),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "ERR_INVALID_CREDENTIALS", "Invalid account");
    ;

    HttpStatus httpStatus;
    String errorCode;
    String message;

    ErrorCode(HttpStatus httpStatus, String errorCode, String message) {
        this.httpStatus = httpStatus;
        this.errorCode = errorCode;
        this.message = message;
    }
}
