package com.novastudy.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "novastudy.jwt")
@Getter
@Setter
public class TokenProperties {
    private String secret;
    private long accessTokenValidateSeconds;
    private long refreshTokenValidateSeconds;
}
