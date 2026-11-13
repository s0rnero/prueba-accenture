package com.accenture.franchises.contracts.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record UserCreateRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 72)
        @Pattern(regexp = PASSWORD_POLICY, message = PASSWORD_MESSAGE) String password,
        @NotBlank @Size(max = 120) String displayName,
        List<String> roles) {

    public static final String PASSWORD_POLICY = "^(?=.*[0-9])(?=.*[^A-Za-z0-9\\s]).{8,72}$";
    public static final String PASSWORD_MESSAGE =
            "debe tener al menos 8 caracteres, un numero y un simbolo";
}
