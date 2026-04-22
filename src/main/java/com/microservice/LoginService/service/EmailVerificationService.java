package com.microservice.LoginService.service;

import com.microservice.LoginService.entity.EmailVerification;
import com.microservice.LoginService.entity.EmailVerification.OtpType;
import com.microservice.LoginService.entity.User;
import com.microservice.LoginService.exception.ApiException;
import com.microservice.LoginService.repository.EmailVerificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
public class EmailVerificationService {

    @Autowired
    private EmailVerificationRepository emailVerificationRepository;

    @Autowired
    private EmailService emailService;

    @Value("${app.otp.expiry-minutes:5}")
    private int otpExpiryMinutes;

    @Value("${app.otp.reset-expiry-minutes:15}")
    private int resetOtpExpiryMinutes;

    private static final SecureRandom RANDOM = new SecureRandom();

    // ── Send OTPs ─────────────────────────────────────────────────────────────

    /**
     * Generates and sends an email verification OTP.
     * Called right after user creation when an email is present.
     * Silently skips if the user has no email (Option A — no-email users are unaffected).
     */
    @Transactional
    public void sendEmailVerificationOtp(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            log.debug("EmailVerificationService: no email for user '{}' — skipping verification OTP", user.getUsername());
            return;
        }

        String otp = generateOtp();
        persistOtp(user, otp, OtpType.EMAIL_VERIFICATION, otpExpiryMinutes);
        emailService.sendEmailVerificationOtp(user.getEmail(), user.getUsername(), otp);
        log.info("EmailVerificationService: verification OTP sent to user '{}'", user.getUsername());
    }

    /**
     * Generates and sends a password-reset OTP.
     * Only called for users with a verified email — validation is done by the caller (AuthService).
     */
    @Transactional
    public void sendPasswordResetOtp(User user) {
        String otp = generateOtp();
        persistOtp(user, otp, OtpType.PASSWORD_RESET, resetOtpExpiryMinutes);
        emailService.sendPasswordResetOtp(user.getEmail(), user.getUsername(), otp);
        log.info("EmailVerificationService: password-reset OTP sent to user '{}'", user.getUsername());
    }

    // ── Verify OTPs ───────────────────────────────────────────────────────────

    /**
     * Verifies the email-verification OTP for the given user.
     * Throws ApiException on any failure (expired, wrong OTP, already used).
     */
    @Transactional
    public void verifyEmailOtp(User user, String rawOtp) {
        EmailVerification record = emailVerificationRepository
                .findTopByUserAndTypeAndUsedFalseOrderByCreatedAtDesc(user, OtpType.EMAIL_VERIFICATION)
                .orElseThrow(() -> new ApiException(
                        "No pending email verification found. Please contact your administrator.", HttpStatus.BAD_REQUEST));

        if (record.isExpired()) {
            throw new ApiException("OTP has expired. Please request a new one from your administrator.", HttpStatus.BAD_REQUEST);
        }

        if (!record.getOtp().equals(rawOtp)) {
            throw new ApiException("Invalid OTP. Please check and try again.", HttpStatus.BAD_REQUEST);
        }

        record.setUsed(true);
        emailVerificationRepository.save(record);
        log.info("EmailVerificationService: email OTP verified for user '{}'", user.getUsername());
    }

    /**
     * Verifies the password-reset OTP for the given user.
     * Throws ApiException on any failure.
     */
    @Transactional
    public void verifyPasswordResetOtp(User user, String rawOtp) {
        EmailVerification record = emailVerificationRepository
                .findTopByUserAndTypeAndUsedFalseOrderByCreatedAtDesc(user, OtpType.PASSWORD_RESET)
                .orElseThrow(() -> new ApiException(
                        "No pending password reset request found. Please request a new OTP.", HttpStatus.BAD_REQUEST));

        if (record.isExpired()) {
            throw new ApiException("OTP has expired. Please request a new one.", HttpStatus.BAD_REQUEST);
        }

        if (!record.getOtp().equals(rawOtp)) {
            throw new ApiException("Invalid OTP. Please check and try again.", HttpStatus.BAD_REQUEST);
        }

        record.setUsed(true);
        emailVerificationRepository.save(record);
        log.info("EmailVerificationService: password-reset OTP verified for user '{}'", user.getUsername());
    }

    // ── Private Helpers ───────────────────────────────────────────────────────

    private void persistOtp(User user, String otp, OtpType type, int expiryMinutes) {
        // Invalidate any existing pending OTPs for this user + type before issuing a new one
        emailVerificationRepository.invalidateAllPendingByUserAndType(user, type);

        EmailVerification verification = EmailVerification.builder()
                .user(user)
                .otp(otp)
                .type(type)
                .expiresAt(LocalDateTime.now().plusMinutes(expiryMinutes))
                .used(false)
                .build();

        emailVerificationRepository.save(verification);
    }

    private String generateOtp() {
        // Generates a 6-digit OTP, zero-padded (e.g. "048291")
        return String.format("%06d", RANDOM.nextInt(1_000_000));
    }
}
