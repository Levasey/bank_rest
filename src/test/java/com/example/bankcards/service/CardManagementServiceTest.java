package com.example.bankcards.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.UserAccount;
import com.example.bankcards.entity.UserRole;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserAccountRepository;
import com.example.bankcards.util.PanEncryptionService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CardManagementServiceTest {

    private static final UUID CARD_A = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID CARD_B = UUID.fromString("20000000-0000-0000-0000-000000000002");

    @Mock CardRepository cardRepository;
    @Mock UserAccountRepository userAccountRepository;
    @Mock PanEncryptionService panEncryptionService;
    @Mock CurrentUserService currentUserService;

    @InjectMocks CardManagementService cardManagementService;

    private UserAccount owner;

    @BeforeEach
    void setUp() {
        owner = new UserAccount();
        owner.setId(UUID.randomUUID());
        owner.setUsername("user1");
        owner.setRole(UserRole.USER);
    }

    @Test
    void transfer_sameCard_throwsBadRequest() {
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        when(currentUserService.isAdmin()).thenReturn(false);

        var req = new TransferRequest(CARD_A, CARD_A, new BigDecimal("1.00"));

        assertThrows(BadRequestException.class, () -> cardManagementService.transfer(req));
    }

    @Test
    void transfer_insufficientBalance_throws() {
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        when(currentUserService.isAdmin()).thenReturn(false);

        Card from = activeCard(CARD_A, new BigDecimal("5.00"));
        Card to = activeCard(CARD_B, BigDecimal.ZERO);

        when(cardRepository.findLockedById(CARD_A)).thenReturn(Optional.of(from));
        when(cardRepository.findLockedById(CARD_B)).thenReturn(Optional.of(to));

        var req = new TransferRequest(CARD_A, CARD_B, new BigDecimal("10.00"));

        assertThrows(BadRequestException.class, () -> cardManagementService.transfer(req));
    }

    @Test
    void transfer_success_updatesBalances() {
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        when(currentUserService.isAdmin()).thenReturn(false);

        Card from = activeCard(CARD_A, new BigDecimal("100.00"));
        Card to = activeCard(CARD_B, new BigDecimal("20.00"));

        when(cardRepository.findLockedById(CARD_A)).thenReturn(Optional.of(from));
        when(cardRepository.findLockedById(CARD_B)).thenReturn(Optional.of(to));

        cardManagementService.transfer(new TransferRequest(CARD_A, CARD_B, new BigDecimal("15.50")));

        assertEquals(new BigDecimal("84.50"), from.getBalance());
        assertEquals(new BigDecimal("35.50"), to.getBalance());
        verify(cardRepository, times(2)).save(any(Card.class));
    }

    private Card activeCard(UUID id, BigDecimal balance) {
        Card c = new Card();
        c.setId(id);
        c.setUser(owner);
        c.setPanEncrypted("x");
        c.setLastFour("1111");
        c.setHolderName("Test");
        c.setExpiryYear((short) 2099);
        c.setExpiryMonth((short) 12);
        c.setStatus(CardStatus.ACTIVE);
        c.setBalance(balance);
        c.setVersion(0L);
        c.setCreatedAt(Instant.now());
        c.setUpdatedAt(Instant.now());
        return c;
    }
}
