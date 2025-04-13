package com.novastudy.utils;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class TokenErrorResponseUtil {
    public static void sendUnauthorizedResponse(HttpServletResponse response, String message, String errorCode) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"status\": 401, \"message\": \"" + message + "\", \"errorCode\": \"" + errorCode + "\"}");
    }
}
