package com.example.bankcards.dto;

import com.example.bankcards.entity.CardStatus;
import jakarta.validation.constraints.Size;

public record CardUpdateRequest(
        @Size(max = 128) String holderName, CardStatus status) {}
