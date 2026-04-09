package com.example.bankcards.controller;

import com.example.bankcards.dto.CardCreateRequest;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CardStatusPatchRequest;
import com.example.bankcards.dto.CardUpdateRequest;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.service.CardManagementService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@SecurityRequirement(name = "bearer-jwt")
@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardManagementService cardManagementService;

    public CardController(CardManagementService cardManagementService) {
        this.cardManagementService = cardManagementService;
    }

    @GetMapping
    public Page<CardResponse> list(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) CardStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UUID userId) {
        return cardManagementService.list(pageable, status, search, userId);
    }

    @GetMapping("/{id}")
    public CardResponse get(@PathVariable UUID id) {
        return cardManagementService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CardResponse create(@Valid @RequestBody CardCreateRequest request) {
        return cardManagementService.create(request);
    }

    @PutMapping("/{id}")
    public CardResponse update(@PathVariable UUID id, @Valid @RequestBody CardUpdateRequest request) {
        return cardManagementService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        cardManagementService.delete(id);
    }

    @PatchMapping("/{id}/status")
    public CardResponse patchStatus(
            @PathVariable UUID id, @Valid @RequestBody CardStatusPatchRequest request) {
        return cardManagementService.patchStatus(id, request);
    }

    @PostMapping("/{id}/request-block")
    public CardResponse requestBlock(@PathVariable UUID id) {
        return cardManagementService.requestBlock(id);
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void transfer(@Valid @RequestBody TransferRequest request) {
        cardManagementService.transfer(request);
    }
}
