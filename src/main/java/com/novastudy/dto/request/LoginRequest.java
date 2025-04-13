package com.novastudy.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
public class LoginRequest {
    @NotBlank(message = "Username cannot be blank")
    String username;
    @NotBlank(message = "Password cannot be blank")
    String password;
}
