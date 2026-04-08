package com.example.bankcards.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CardCreateRequest(
        @NotBlank @Size(min = 13, max = 19) String pan,
        @NotBlank @Size(max = 128) String holderName,
        @NotNull @Min(1) @Max(12) Integer expiryMonth,
        @NotNull @Min(2000) @Max(2100) Integer expiryYear,
        @NotNull @DecimalMin("0.00") BigDecimal balance,
        @NotNull UUID userId) {

    @AssertTrue(message = "Card number must contain 13-19 digits")
    public boolean isPanDigitsValid() {
        if (pan == null) {
            return false;
        }
        String d = pan.replaceAll("\\D", "");
        return d.length() >= 13 && d.length() <= 19;
    }
}
