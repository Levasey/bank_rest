package com.example.bankcards.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record TransferRequest(
        @NotNull UUID fromCardId,
        @NotNull UUID toCardId,
        @NotNull @DecimalMin(value = "0.01", message = "Amount must be positive")
                BigDecimal amount) {}
