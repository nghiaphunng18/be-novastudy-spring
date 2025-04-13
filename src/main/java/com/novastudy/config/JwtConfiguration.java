package com.novastudy.config;

import com.novastudy.utils.TokenSecurityUtil;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.util.Base64;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.util.Collections;

@Configuration
public class JwtConfiguration {
    private final TokenProperties tokenProperties;

    public JwtConfiguration(TokenProperties tokenProperties) {
        this.tokenProperties = tokenProperties;
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Base64.from(tokenProperties.getSecret()).decode();
        return new SecretKeySpec(keyBytes, 0, keyBytes.length, TokenSecurityUtil.JWT_ALGORITHM.getName());
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(getSecretKey()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withSecretKey(
                getSecretKey()).macAlgorithm(TokenSecurityUtil.JWT_ALGORITHM).build();

        // set clock skew return 10
        jwtDecoder.setJwtValidator(
                new DelegatingOAuth2TokenValidator<>(
                        Collections.singletonList(new JwtTimestampValidator(Duration.ofSeconds(10)))
                )
        );

        return token -> {
            try {
                return jwtDecoder.decode(token);
            } catch (Exception e) {
                System.out.println(">>> JWT error: " + e.getMessage());
                throw  e;
            }
        };
    }
}
