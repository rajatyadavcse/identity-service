package com.microservice.LoginService.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyEmailRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "OTP is required")
    private String otp;
}
