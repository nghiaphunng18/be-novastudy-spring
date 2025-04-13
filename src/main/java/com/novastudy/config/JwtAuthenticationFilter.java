package com.novastudy.config;

import com.novastudy.utils.PublicEndpoints;
import com.novastudy.utils.TokenErrorResponseUtil;
import com.novastudy.utils.TokenSecurityUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter implements Ordered {
    private static final Logger logger = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtDecoder jwtDecoder;
    private final TokenSecurityUtil tokenSecurityUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (PublicEndpoints.isPublic(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get access token from Authorization header
        String accessToken = tokenSecurityUtil.extractTokenFromHeader(request);
        if (accessToken == null) {
            logger.warn("Missing or invalid Authorization header for path: {}", path);
            TokenErrorResponseUtil.sendUnauthorizedResponse(response, "Missing or invalid access token", "ERR_INVALID_TOKEN");
            return;
        }
        try {
            tokenSecurityUtil.validateAndSetAuthentication(accessToken, jwtDecoder);
            logger.debug("Authenticated user: {} for path: {}", SecurityContextHolder.getContext().getAuthentication().getName(), path);
            filterChain.doFilter(request, response);
        } catch (JwtException e) {
            logger.warn("Failed to validate JWT token: {} for path: {}. Error: {}", accessToken, path, e.getMessage());
            TokenErrorResponseUtil.sendUnauthorizedResponse(response, "Invalid or expired access token", "ERR_INVALID_TOKEN");
        }
    }

    @Override
    public int getOrder() {
        return 75;
    }
}
