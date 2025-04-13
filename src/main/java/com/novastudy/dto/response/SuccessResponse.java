package com.novastudy.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SuccessResponse<T> {
    private LocalDateTime timestamp;
    private int status;
    private String message;
    private T data;
}