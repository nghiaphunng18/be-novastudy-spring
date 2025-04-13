package com.aims.utils;

import com.aims.config.TokenProperties;
import com.aims.dto.response.LoginResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class TokenSecurityUtil {
    private final JwtEncoder jwtEncoder;
    private final TokenProperties tokenProperties;
    public static final MacAlgorithm JWT_ALGORITHM = MacAlgorithm.HS256;

    public static Optional<String> getCurrentUserLogin() {
        SecurityContext securityContext = SecurityContextHolder.getContext();
        return Optional.ofNullable(securityContext.getAuthentication())
                .filter(Authentication::isAuthenticated)
                .map(TokenSecurityUtil::extractPrincipal);
    }

    private static String extractPrincipal(Authentication authentication) {
        if (authentication == null) {
            return null;
        } else if (authentication.getPrincipal() instanceof UserDetails springSecurityUser) {
            return springSecurityUser.getUsername();
        } else if (authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getSubject();
        } else if (authentication.getPrincipal() instanceof String s) {
            return s;
        }
        return null;
    }

    public TokenSecurityUtil(JwtEncoder jwtEncoder, TokenProperties tokenProperties) {
        this.jwtEncoder = jwtEncoder;
        this.tokenProperties = tokenProperties;
    }

    public String createAccessToken(String userName, Set<String> authorities) {
        Instant now = Instant.now();
        Instant validity = now.plus(tokenProperties.getAccessTokenValidateSeconds(), ChronoUnit.SECONDS);

        JwtClaimsSet jwtClaimsSet = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(userName)
                .claim("token_type", "access")
                .claim("authorities", authorities)
                .build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();

        return this.jwtEncoder.encode(JwtEncoderParameters
                .from(jwsHeader, jwtClaimsSet)).getTokenValue();
    }

    public String createRefreshToken(String userName) {
        Instant now = Instant.now();
        Instant validity = now.plus(tokenProperties.getRefreshTokenValidateSeconds(), ChronoUnit.SECONDS);

        JwtClaimsSet jwtClaimsSet = JwtClaimsSet.builder()
                .issuedAt(now)
                .expiresAt(validity)
                .subject(userName)
                .claim("token_type", "refresh")
                .build();

        JwsHeader jwsHeader = JwsHeader.with(JWT_ALGORITHM).build();

        return this.jwtEncoder.encode(JwtEncoderParameters
                .from(jwsHeader, jwtClaimsSet)).getTokenValue();
    }

    public String extractTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        return authHeader.substring(7);
    }

    public String extractRefreshTokenFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        return Arrays.stream(cookies)
                .filter(cookie -> "refresh_token".equals(cookie.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    public ResponseCookie createRefreshTokenCookie(String refreshToken, long maxAge) {
        return ResponseCookie.from("refresh_token", refreshToken)
                .secure(true)
                .httpOnly(true)
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    public void validateAndSetAuthentication(String accessToken, JwtDecoder jwtDecoder) throws JwtException {
        Jwt jwt = jwtDecoder.decode(accessToken);
        String username = jwt.getSubject();
        List<String> authorities = jwt.getClaimAsStringList("authorities");
        if (username == null || authorities == null) {
            throw new JwtException("Invalid JWT token: missing username or authorities");
        }
        List<SimpleGrantedAuthority> grantedAuthorities = authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, null, grantedAuthorities);
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
}
