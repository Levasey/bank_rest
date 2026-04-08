package com.example.bankcards.service;

import com.example.bankcards.dto.CardCreateRequest;
import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.CardStatusPatchRequest;
import com.example.bankcards.dto.CardUpdateRequest;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.UserAccount;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ForbiddenException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserAccountRepository;
import com.example.bankcards.util.CardMaskFormatter;
import com.example.bankcards.util.CardPanNormalizer;
import com.example.bankcards.util.PanEncryptionService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.time.YearMonth;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CardManagementService {

    private final CardRepository cardRepository;
    private final UserAccountRepository userAccountRepository;
    private final PanEncryptionService panEncryptionService;
    private final CurrentUserService currentUserService;

    public CardManagementService(
            CardRepository cardRepository,
            UserAccountRepository userAccountRepository,
            PanEncryptionService panEncryptionService,
            CurrentUserService currentUserService) {
        this.cardRepository = cardRepository;
        this.userAccountRepository = userAccountRepository;
        this.panEncryptionService = panEncryptionService;
        this.currentUserService = currentUserService;
    }

    @Transactional
    public Page<CardResponse> list(Pageable pageable, CardStatus status, String search, UUID userIdFilter) {
        UUID owner = null;
        if (!currentUserService.isAdmin()) {
            owner = currentUserService.requireCurrentUser().getId();
        } else if (userIdFilter != null) {
            owner = userIdFilter;
        }
        var spec = CardSpecifications.filtered(owner, status, search);
        return cardRepository.findAll(spec, pageable).map(this::toResponseAfterExpiryCheck);
    }

    @Transactional
    public CardResponse getById(UUID cardId) {
        Card card =
                cardRepository
                        .findDetailById(cardId)
                        .orElseThrow(() -> new NotFoundException("Card not found"));
        assertCanView(card);
        return toResponseAfterExpiryCheck(card);
    }

    @Transactional
    public CardResponse create(CardCreateRequest request) {
        UserAccount owner =
                userAccountRepository
                        .findById(request.userId())
                        .orElseThrow(() -> new NotFoundException("Card owner not found"));
        String digits = CardPanNormalizer.digitsOnly(request.pan());
        String lastFour = CardPanNormalizer.lastFour(digits);
        String encrypted;
        try {
            encrypted = panEncryptionService.encrypt(digits);
        } catch (GeneralSecurityException e) {
            throw new BadRequestException("Unable to encrypt card data");
        }
        Instant now = Instant.now();
        Card card = new Card();
        card.setId(UUID.randomUUID());
        card.setUser(owner);
        card.setPanEncrypted(encrypted);
        card.setLastFour(lastFour);
        card.setHolderName(request.holderName().trim());
        card.setExpiryMonth(request.expiryMonth().shortValue());
        card.setExpiryYear(request.expiryYear().shortValue());
        card.setStatus(initialStatusForExpiry(request.expiryMonth(), request.expiryYear()));
        card.setBalance(request.balance().setScale(2, RoundingMode.HALF_UP));
        card.setVersion(0L);
        card.setCreatedAt(now);
        card.setUpdatedAt(now);
        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public CardResponse update(UUID cardId, CardUpdateRequest request) {
        Card card =
                cardRepository
                        .findDetailById(cardId)
                        .orElseThrow(() -> new NotFoundException("Card not found"));
        if (request.holderName() != null && !request.holderName().isBlank()) {
            card.setHolderName(request.holderName().trim());
        }
        if (request.status() != null) {
            card.setStatus(request.status());
        }
        card.setUpdatedAt(Instant.now());
        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public void delete(UUID cardId) {
        if (!cardRepository.existsById(cardId)) {
            throw new NotFoundException("Card not found");
        }
        cardRepository.deleteById(cardId);
    }

    @Transactional
    public CardResponse patchStatus(UUID cardId, CardStatusPatchRequest request) {
        Card card =
                cardRepository
                        .findDetailById(cardId)
                        .orElseThrow(() -> new NotFoundException("Card not found"));
        card.setStatus(request.status());
        card.setUpdatedAt(Instant.now());
        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public CardResponse requestBlock(UUID cardId) {
        UserAccount user = currentUserService.requireCurrentUser();
        Card card =
                cardRepository
                        .findDetailById(cardId)
                        .orElseThrow(() -> new NotFoundException("Card not found"));
        if (!card.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("You can only block your own cards");
        }
        if (card.getStatus() == CardStatus.EXPIRED) {
            throw new BadRequestException("Cannot change status of an expired card");
        }
        card.setStatus(CardStatus.BLOCKED);
        card.setUpdatedAt(Instant.now());
        return toResponse(cardRepository.save(card));
    }

    @Transactional
    public void transfer(TransferRequest request) {
        UserAccount actor = currentUserService.requireCurrentUser();
        boolean admin = currentUserService.isAdmin();
        if (request.fromCardId().equals(request.toCardId())) {
            throw new BadRequestException("Source and destination cards must be different");
        }
        UUID first =
                request.fromCardId().compareTo(request.toCardId()) < 0
                        ? request.fromCardId()
                        : request.toCardId();
        UUID second = first.equals(request.fromCardId()) ? request.toCardId() : request.fromCardId();

        Card locked1 =
                cardRepository
                        .findLockedById(first)
                        .orElseThrow(() -> new NotFoundException("Card not found"));
        Card locked2 =
                cardRepository
                        .findLockedById(second)
                        .orElseThrow(() -> new NotFoundException("Card not found"));
        Card from = first.equals(request.fromCardId()) ? locked1 : locked2;
        Card to = first.equals(request.fromCardId()) ? locked2 : locked1;

        if (!admin
                && (!from.getUser().getId().equals(actor.getId())
                        || !to.getUser().getId().equals(actor.getId()))) {
            throw new ForbiddenException("Transfers are allowed only between your own cards");
        }
        applyExpiryTransition(from);
        applyExpiryTransition(to);
        if (from.getStatus() != CardStatus.ACTIVE || to.getStatus() != CardStatus.ACTIVE) {
            throw new BadRequestException("Both cards must be active");
        }
        BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);
        if (from.getBalance().compareTo(amount) < 0) {
            throw new BadRequestException("Insufficient balance");
        }
        Instant now = Instant.now();
        from.setBalance(from.getBalance().subtract(amount));
        to.setBalance(to.getBalance().add(amount));
        from.setUpdatedAt(now);
        to.setUpdatedAt(now);
        cardRepository.save(from);
        cardRepository.save(to);
    }

    private void assertCanView(Card card) {
        if (currentUserService.isAdmin()) {
            return;
        }
        UserAccount user = currentUserService.requireCurrentUser();
        if (!card.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException("Access denied");
        }
    }

    private CardResponse toResponseAfterExpiryCheck(Card card) {
        applyExpiryTransition(card);
        return toResponse(card);
    }

    private void applyExpiryTransition(Card card) {
        if (card.getStatus() != CardStatus.ACTIVE) {
            return;
        }
        YearMonth expiry = YearMonth.of(card.getExpiryYear(), card.getExpiryMonth());
        if (expiry.isBefore(YearMonth.now())) {
            card.setStatus(CardStatus.EXPIRED);
            card.setUpdatedAt(Instant.now());
            cardRepository.save(card);
        }
    }

    private CardStatus initialStatusForExpiry(int month, int year) {
        YearMonth expiry = YearMonth.of(year, month);
        if (expiry.isBefore(YearMonth.now())) {
            return CardStatus.EXPIRED;
        }
        return CardStatus.ACTIVE;
    }

    private CardResponse toResponse(Card card) {
        return new CardResponse(
                card.getId(),
                CardMaskFormatter.maskLastFour(card.getLastFour()),
                card.getHolderName(),
                card.getExpiryMonth(),
                card.getExpiryYear(),
                card.getStatus(),
                card.getBalance(),
                card.getUser().getId(),
                card.getCreatedAt(),
                card.getUpdatedAt());
    }
}
