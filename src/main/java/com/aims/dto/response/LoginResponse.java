package com.aims.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private UserLoginResponse userLoginResponse;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UserLoginResponse {
        private String userId;
        private String email;
        private String userName;
        private String fullName;
        private String createdAt;
        private String updatedAt;
        private Set<String> authorities;
    }
}
