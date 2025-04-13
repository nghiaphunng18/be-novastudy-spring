package com.novastudy.config;

import com.novastudy.enums.ErrorCode;
import com.novastudy.exception.ErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper mapper;
    private final AuthenticationEntryPoint delegate = new BearerTokenAuthenticationEntryPoint();

    public CustomAuthenticationEntryPoint(ObjectMapper mapper){
        this.mapper = mapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        this.delegate.commence(request, response, authException);
        response.setContentType("application/json;charset=UTF-8");

        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        String errorMessage = Optional.ofNullable(authException.getCause())
                .map(Throwable::getMessage)
                .orElse(authException.getMessage());

        ErrorResponse errorResponse = ErrorResponse.builder()
                .status(errorCode.getHttpStatus().value())
                .message(errorMessage)
                .path(request.getRequestURI())
                .errorCode(errorCode.getErrorCode())
                .timestamp(LocalDateTime.now())
                .build();

        mapper.writeValue(response.getWriter(), errorResponse);
    }
}
