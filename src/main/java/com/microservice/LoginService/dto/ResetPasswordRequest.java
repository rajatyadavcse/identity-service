package com.microservice.LoginService.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "New password is required")
    private String newPassword;
}
