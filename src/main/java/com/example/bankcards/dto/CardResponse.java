package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CardResponse(
        UUID id,
        String maskedPan,
        String holderName,
        int expiryMonth,
        int expiryYear,
        CardStatus status,
        BigDecimal balance,
        UUID userId,
        Instant createdAt,
        Instant updatedAt) {}
