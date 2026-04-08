package com.example.bankcards.dto;

public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {}
