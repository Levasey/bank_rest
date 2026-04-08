package com.example.bankcards.dto;

import com.example.bankcards.entity.UserRole;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id, String username, String email, UserRole role, boolean enabled, Instant createdAt) {}
