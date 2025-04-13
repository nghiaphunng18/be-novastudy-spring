package com.aims.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfoUserResponse {
    private String userId;
    private String email;
    private String userName;
    private String fullName;
    private String createdAt;
    private String updatedAt;
    private Set<String> authorities;
}
