package com.aims.controller;

import com.aims.config.TokenProperties;
import com.aims.dto.request.LoginRequest;
import com.aims.dto.request.RegisterAccountRequest;
import com.aims.dto.response.*;
import com.aims.exception.AppException;
import com.aims.service.UserService;
import com.aims.utils.TokenSecurityUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.apache.catalina.security.SecurityUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Arrays;

@RestController
@FieldDefaults(level = AccessLevel.PRIVATE)
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {
    final AuthenticationManagerBuilder authenticationManagerBuilder;
    final TokenSecurityUtil tokenSecurityUtil;
    final UserService userService;
    private final TokenProperties tokenProperties;

    @PostMapping("/register")
    public ResponseEntity<SuccessResponse<RegisterAccountResponse>> register(@Valid @RequestBody RegisterAccountRequest request) {
        RegisterAccountResponse registerResponse = userService.register(request);

        SuccessResponse<RegisterAccountResponse> response = SuccessResponse.<RegisterAccountResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .message("Create new account successful")
                .data(registerResponse)
                .build();

        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<SuccessResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        try {
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword());
            Authentication authentication = authenticationManagerBuilder.getObject().authenticate(authenticationToken);

            SecurityContextHolder.getContext().setAuthentication(authentication);

            LoginResponse.UserLoginResponse userLoginResponse = userService.findByUsername(authentication.getName());
            LoginResponse loginResponse = new LoginResponse();
            loginResponse.setUserLoginResponse(userLoginResponse);

            // create access token
            String accessToken = this.tokenSecurityUtil.createAccessToken(authentication.getName(), userLoginResponse.getAuthorities());
            loginResponse.setAccessToken(accessToken);

            // create refresh token
            String refreshToken = this.tokenSecurityUtil.createRefreshToken(authentication.getName());
            loginResponse.setRefreshToken(refreshToken);
            String deviceInfo = httpServletRequest.getHeader("User-Agent");
            userService.updateRefreshTokenUser(refreshToken, authenticationToken.getName(), deviceInfo);

            // set cookie
            ResponseCookie responseCookie = tokenSecurityUtil.createRefreshTokenCookie(refreshToken, tokenProperties.getRefreshTokenValidateSeconds());

            SuccessResponse<LoginResponse> response = SuccessResponse.<LoginResponse>builder()
                    .timestamp(LocalDateTime.now())
                    .status(200)
                    .message("Login successful")
                    .data(loginResponse)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                    .body(response);
        } catch (AuthenticationException exception) {
            throw AppException.invalidCredentials();
        }
    }

    @GetMapping("/my-account")
    @PreAuthorize("hasAuthority('VIEW_PROFILE')")
    public ResponseEntity<SuccessResponse<InfoUserResponse>> getInfoAccount() {
        InfoUserResponse infoUserResponse = userService.getCurrentUserInfo();

        SuccessResponse<InfoUserResponse> response = SuccessResponse.<InfoUserResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .message("Get information successful")
                .data(infoUserResponse)
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/refresh-token")
    public ResponseEntity<SuccessResponse<RefreshAccessTokenResponse>> refreshAccessTokenResponse(HttpServletRequest httpServletRequest) {
        String refreshToken = tokenSecurityUtil.extractRefreshTokenFromCookies(httpServletRequest);

        if (refreshToken == null) {
            throw AppException.unauthorized("Refresh token is missing");
        }

        // get new access token
        RefreshAccessTokenResponse refreshTokenResponse = userService.refreshToken(refreshToken);

        ResponseCookie responseCookie = tokenSecurityUtil.createRefreshTokenCookie(refreshToken, tokenProperties.getRefreshTokenValidateSeconds());

        SuccessResponse<RefreshAccessTokenResponse> response = SuccessResponse.<RefreshAccessTokenResponse>builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .message("Access Token refreshed successfully")
                .data(refreshTokenResponse)
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<SuccessResponse<Void>> logout(HttpServletRequest request) {
        // Get access token from Authorization header
        String accessToken = tokenSecurityUtil.extractTokenFromHeader(request); // Remove "Bearer " prefix
        if (accessToken == null) {
            throw AppException.unauthorized("Access token is missing");
        }

        // Get refresh token from cookie
        String refreshToken = tokenSecurityUtil.extractRefreshTokenFromCookies(request);
        if (refreshToken == null) {
            throw AppException.unauthorized("Refresh token is missing");
        }

        // Invalidate both access token and refresh token
        userService.logout(refreshToken, accessToken);

        // Clear the refresh token cookie
        ResponseCookie responseCookie = ResponseCookie
                .from("refresh_token", null)
                .secure(true)
                .httpOnly(true)
                .path("/")
                .maxAge(0)
                .build();

        SuccessResponse<Void> response = SuccessResponse.<Void>builder()
                .timestamp(LocalDateTime.now())
                .status(200)
                .message("Logout successful")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, responseCookie.toString())
                .body(response);
    }
}
