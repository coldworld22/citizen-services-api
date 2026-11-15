package io.github.coldworld22.eservices.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

record OtpRequest(
        @NotBlank @Pattern(regexp = "[12]\\d{9}", message = "must be a 10-digit national or residence ID")
        String nationalId) {}

record OtpVerifyRequest(
        @NotBlank @Pattern(regexp = "[12]\\d{9}", message = "must be a 10-digit national or residence ID")
        String nationalId,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "must be 6 digits")
        String code) {}

/**
 * @param devCode only present when app.otp.expose-code is enabled (local demo); never in production
 */
record OtpRequestResponse(String message, long expiresInSeconds, String devCode) {}
