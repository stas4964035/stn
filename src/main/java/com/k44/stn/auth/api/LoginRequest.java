package com.k44.stn.auth.api;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank(message = "email не может быть пустым")
        @Email(message = "должен быть правильно оформленный email-адрес")
        String email,

        @NotBlank(message = "пароль не может быть пустым")
        @Size(min = 6, max = 255, message = "длина пароля должна быть не менее 6 и не более 255 символов")
        String password
) {
}
