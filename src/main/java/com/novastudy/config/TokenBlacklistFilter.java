package com.novastudy.config;

import com.novastudy.repository.TokenBlacklistRepository;
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
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class TokenBlacklistFilter extends OncePerRequestFilter implements Ordered {
    private static final Logger logger = LoggerFactory.getLogger(TokenBlacklistFilter.class);
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final TokenSecurityUtil tokenSecurityUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (PublicEndpoints.isPublic(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = tokenSecurityUtil.extractTokenFromHeader(request);
        // check token in blacklist token
        if (tokenBlacklistRepository.findByToken(accessToken).isPresent()) {
            logger.warn("Access token is blacklisted: {}. Request path: {}", accessToken, path);
            TokenErrorResponseUtil.sendUnauthorizedResponse(response, "Access token has been revoked", "ERR_TOKEN_REVOKED");
            return;
        }

        filterChain.doFilter(request, response);
    }

    @Override
    public int getOrder() {
        return 50;
    }
}
