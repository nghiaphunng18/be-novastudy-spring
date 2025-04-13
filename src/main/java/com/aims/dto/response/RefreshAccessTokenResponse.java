package com.aims.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshAccessTokenResponse {
    private String accessToken;
}
