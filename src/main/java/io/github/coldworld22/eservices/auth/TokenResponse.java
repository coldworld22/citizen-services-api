package io.github.coldworld22.eservices.auth;

public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds, String role) {}
