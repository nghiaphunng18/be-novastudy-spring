package com.aims.service;

import com.aims.dto.request.RegisterAccountRequest;
import com.aims.dto.response.InfoUserResponse;
import com.aims.dto.response.LoginResponse;
import com.aims.dto.response.RefreshAccessTokenResponse;
import com.aims.dto.response.RegisterAccountResponse;
import com.aims.entity.*;
import com.aims.enums.TokenStatus;
import com.aims.exception.AppException;
import com.aims.repository.RefreshTokenRepository;
import com.aims.repository.RoleRepository;
import com.aims.repository.TokenBlacklistRepository;
import com.aims.repository.UserRepository;
import com.aims.utils.TokenSecurityUtil;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    @Value("${aims.jwt.refresh-token-validate-seconds}")
    private long refreshTokenValidateSeconds;

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenSecurityUtil tokenSecurityUtil;
    private final JwtDecoder jwtDecoder;
    private final TokenBlacklistRepository tokenBlacklistRepository;

    private Set<String> extractAuthoritiesFromUser(User user) {
        Hibernate.initialize(user.getRoles());
        user.getRoles().forEach(role -> Hibernate.initialize(role.getPermissions()));
        Set<String> roles = user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        Set<String> permissions = user.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .map(Permission::getName)
                .collect(Collectors.toSet());
        Set<String> authorities = new HashSet<>();
        authorities.addAll(roles);
        authorities.addAll(permissions);
        return authorities;
    }

    public LoginResponse.UserLoginResponse findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username and password"));

        Set<String> authorities = extractAuthoritiesFromUser(user);

        return LoginResponse.UserLoginResponse.builder()
                .userId(user.getId())
                .userName(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .createdAt(user.getCreatedAt().toString())
                .updatedAt(user.getCreatedAt().toString())
                .authorities(authorities)
                .build();
    }

    public void updateRefreshTokenUser(String refreshToken, String username, String deviceInfo) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> AppException.resourceNotFound("User not found"));

        LocalDateTime issuedAt = LocalDateTime.now();
        LocalDateTime expiresAt = issuedAt.plusSeconds(refreshTokenValidateSeconds);
        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(refreshToken)
                .sessionId(UUID.randomUUID().toString())
                .deviceInfo(deviceInfo != null ? deviceInfo : "Unknown")
                .status(TokenStatus.VALID)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .build();

        //save refresh token
        refreshTokenRepository.save(token);
    }

    public InfoUserResponse getCurrentUserInfo() {
        String username = TokenSecurityUtil.getCurrentUserLogin()
                .orElseThrow(() -> AppException.unauthorized("User not authenticated"));
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> AppException.resourceNotFound("User not found"));

        Set<String> authorities = extractAuthoritiesFromUser(user);

        return InfoUserResponse.builder()
                .userId(user.getId())
                .userName(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .createdAt(user.getCreatedAt().toString())
                .updatedAt(user.getUpdatedAt().toString())
                .authorities(authorities)
                .build();
    }

    public RegisterAccountResponse register(RegisterAccountRequest request) {
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw AppException.conflict("Username already exists");
        }
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw AppException.conflict("Email already exists");
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .build();

        Role role = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> AppException.resourceNotFound("ROLE_USER not found"));
        user.setRoles(new HashSet<>());
        user.getRoles().add(role);

        userRepository.save(user);

        return RegisterAccountResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .build();
    }

    public RefreshAccessTokenResponse refreshToken(String refreshToken) {
        RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> AppException.unauthorized("invalid refresh token"));

        // check token status + expiration
        if (refreshTokenEntity.getStatus() != TokenStatus.VALID) {
            throw AppException.unauthorized("Refresh token is not valid");
        }
        if (refreshTokenEntity.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenEntity.setStatus(TokenStatus.INVALID);
            refreshTokenRepository.save(refreshTokenEntity);
            throw AppException.unauthorized("Refresh token has expired");
        }

        //get info user
        User user = refreshTokenEntity.getUser();
        Set<String> authorities = extractAuthoritiesFromUser(user);

        String accessToken = tokenSecurityUtil.createAccessToken(user.getUsername(), authorities);

        return RefreshAccessTokenResponse.builder()
                .accessToken(accessToken)
                .build();
    }

    public void logout(String refreshToken, String accessToken) {
        logger.info("User logout attempt with refresh token: {}", refreshToken);

        // Get refresh token entity to retrieve session_id and device_info
        RefreshToken tokenEntity = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> {
                    logger.error("Invalid refresh token during logout: {}", refreshToken);
                    return AppException.unauthorized("Invalid refresh token");
                });

        String sessionId = tokenEntity.getSessionId();
        String deviceInfo = tokenEntity.getDeviceInfo();
        String username = tokenEntity.getUser().getUsername();

        // Blacklist the access token
        Jwt jwt = jwtDecoder.decode(accessToken);
        Instant expiresAtInstant = jwt.getExpiresAt();
        LocalDateTime expiresAt = LocalDateTime.ofInstant(expiresAtInstant, java.time.ZoneId.systemDefault());

        TokenBlackList blacklistedToken = TokenBlackList.builder()
                .token(accessToken)
                .expiresAt(expiresAt)
                .build();
        tokenBlacklistRepository.save(blacklistedToken);
        logger.debug("Access token blacklisted: {} for user: {}, session: {}, device: {}",
                accessToken, username, sessionId, deviceInfo);

        // Invalidate the refresh token
        tokenEntity.setStatus(TokenStatus.INVALID);
        refreshTokenRepository.save(tokenEntity);
        logger.info("Refresh token invalidated: {} for user: {}, session: {}, device: {}",
                refreshToken, username, sessionId, deviceInfo);
    }
}
