package com.microservice.LoginService.service;

import com.microservice.LoginService.dto.*;
import com.microservice.LoginService.entity.User;
import com.microservice.LoginService.exception.ApiException;
import com.microservice.LoginService.repository.UserRepository;
import com.microservice.LoginService.security.JwtUtil;
import com.microservice.LoginService.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    // In-memory store: username → refresh token (lost on restart)
    private final Map<String, String> refreshTokenStore = new ConcurrentHashMap<>();

    @Autowired
    private EmailVerificationService emailVerificationService;

    // ── Login ─────────────────────────────────────────────────────────────────

    public AuthTokens login(LoginRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        User user = principal.getUser();

        // Block login if email is set but not yet verified
        if (user.getEmail() != null && !user.getEmail().isBlank() && !Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new ApiException(
                    "Email not verified. Please check your inbox for the verification OTP.",
                    HttpStatus.FORBIDDEN);
        }

        String accessToken  = jwtUtil.generateAccessToken(user.getUsername(), user.getRole(), user.getRestaurantId());
        String refreshToken = jwtUtil.generateRefreshToken(user.getUsername());
        refreshTokenStore.put(user.getUsername(), refreshToken);

        return new AuthTokens(accessToken, refreshToken, "ROLE_" + user.getRole().name());
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────
    // Token is read from the HttpOnly cookie by the controller; the raw value is passed here.

    public AuthTokens refreshToken(String refreshToken) {
        String username;
        try {
            username = jwtUtil.extractUsername(refreshToken);
        } catch (Exception e) {
            throw new ApiException("Invalid refresh token", HttpStatus.UNAUTHORIZED);
        }

        if (jwtUtil.isTokenExpired(refreshToken)) {
            refreshTokenStore.remove(username);
            throw new ApiException("Refresh token expired. Please login again.", HttpStatus.UNAUTHORIZED);
        }

        String storedToken = refreshTokenStore.get(username);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new ApiException("Refresh token is invalid or already used", HttpStatus.UNAUTHORIZED);
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        String newAccessToken  = jwtUtil.generateAccessToken(user.getUsername(), user.getRole(), user.getRestaurantId());
        String newRefreshToken = jwtUtil.generateRefreshToken(user.getUsername());
        refreshTokenStore.put(username, newRefreshToken);

        return new AuthTokens(newAccessToken, newRefreshToken, "ROLE_" + user.getRole().name());
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    public void logout(String username) {
        refreshTokenStore.remove(username);
    }

    // ── Reset Password (Admin-only) ───────────────────────────────────────────

    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ApiException("User not found: " + request.getUsername(), HttpStatus.NOT_FOUND));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenStore.remove(user.getUsername());
    }

    // ── Change Password (Self-service) ────────────────────────────────────────

    public void changePassword(String username, ChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new ApiException("Current password is incorrect", HttpStatus.BAD_REQUEST);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        refreshTokenStore.remove(username);
    }

    // ── Verify Email ──────────────────────────────────────────────────────────

    public void verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        if (Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new ApiException("Email is already verified.", HttpStatus.BAD_REQUEST);
        }

        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new ApiException("No email address associated with this account.", HttpStatus.BAD_REQUEST);
        }

        emailVerificationService.verifyEmailOtp(user, request.getOtp());

        user.setIsEmailVerified(true);
        userRepository.save(user);
        log.info("AuthService: email verified for user '{}'", user.getUsername());
    }

    // ── Forgot Password ───────────────────────────────────────────────────────

    /**
     * Initiates a password-reset OTP flow.
     * Always returns without revealing whether the email exists (prevents enumeration).
     */
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            if (!Boolean.TRUE.equals(user.getIsEmailVerified())) {
                // Email is registered but not verified — silently skip (can't authenticate the user)
                log.warn("AuthService: forgot-password requested for unverified email '{}'", request.getEmail());
                return;
            }
            emailVerificationService.sendPasswordResetOtp(user);
        });
        // Always succeed — caller gets a generic response regardless of email existence
    }

    // ── Reset Password with OTP ────────────────────────────────────────────────

    public void resetPasswordWithOtp(ResetPasswordWithOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ApiException(
                        "No account found with this email address.", HttpStatus.NOT_FOUND));

        if (!Boolean.TRUE.equals(user.getIsEmailVerified())) {
            throw new ApiException(
                    "Email is not verified. Password reset is not available for this account.",
                    HttpStatus.FORBIDDEN);
        }

        emailVerificationService.verifyPasswordResetOtp(user, request.getOtp());

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidate all active sessions for this user
        refreshTokenStore.remove(user.getUsername());
        log.info("AuthService: password reset via OTP for user '{}'", user.getUsername());
    }
}
