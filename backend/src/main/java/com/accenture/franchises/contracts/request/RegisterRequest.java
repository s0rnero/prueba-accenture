package com.accenture.franchises.contracts.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "El correo es obligatorio.")
        @Email(message = "El correo no es valido.")
        String email,
        @NotBlank(message = "La contrasena es obligatoria.")
        @Size(min = 8, max = 72, message = "La contrasena debe tener entre 8 y 72 caracteres.")
        @Pattern(regexp = UserCreateRequest.PASSWORD_POLICY,
                message = UserCreateRequest.PASSWORD_MESSAGE) String password,
        @NotBlank(message = "El nombre es obligatorio.")
        @Size(max = 120, message = "El nombre no puede superar 120 caracteres.")
        String displayName) {
}
