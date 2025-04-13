package com.novastudy.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class CustomJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        // validate token type
        String tokenType = jwt.getClaimAsString("token_type");
        if (tokenType == null || !tokenType.equals("access")) {
            throw new IllegalArgumentException("Invalid token type: must be access_token");
        }

        // extract authorities
        List<String> authorities= jwt.getClaimAsStringList("authorities") != null ?
                jwt.getClaimAsStringList("authorities") : Collections.emptyList();

        List<GrantedAuthority> grantedAuthorities = authorities.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());

        return new JwtAuthenticationToken(jwt, grantedAuthorities);
    }
}
