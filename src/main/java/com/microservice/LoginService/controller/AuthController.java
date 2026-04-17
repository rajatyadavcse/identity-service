package com.microservice.LoginService.controller;

import com.microservice.LoginService.dto.*;
import com.microservice.LoginService.entity.User;
import com.microservice.LoginService.security.UserPrincipal;
import com.microservice.LoginService.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "Login, token management, and password operations")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Login with username and password")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Obtain new access token using a valid refresh token")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and invalidate refresh token",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<MessageResponse> logout(@AuthenticationPrincipal UserPrincipal principal) {
        authService.logout(principal.getUsername());
        return ResponseEntity.ok(new MessageResponse("Logged out successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user info",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<MeResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        User user = principal.getUser();
        return ResponseEntity.ok(MeResponse.from(user));
    }

    @PostMapping("/reset-password")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Reset another user's password (ADMIN / SUPER_ADMIN only)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(new MessageResponse("Password reset successfully"));
    }

    @PatchMapping("/change-password")
    @Operation(summary = "Change own password (any authenticated user)",
            security = @SecurityRequirement(name = "bearerAuth"))
    public ResponseEntity<MessageResponse> changePassword(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(principal.getUsername(), request);
        return ResponseEntity.ok(new MessageResponse("Password changed successfully"));
    }
}
