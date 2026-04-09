package com.example.bankcards.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bankcards.dto.CardResponse;
import com.example.bankcards.dto.TransferRequest;
import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.UserAccount;
import com.example.bankcards.entity.UserRole;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.ForbiddenException;
import com.example.bankcards.repository.CardRepository;
import com.example.bankcards.repository.UserAccountRepository;
import com.example.bankcards.util.PanEncryptionService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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

        lenient()
                .when(cardRepository.save(any(Card.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
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

    @Test
    void transfer_destinationBelongsToAnotherUser_forbidden() {
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        when(currentUserService.isAdmin()).thenReturn(false);

        Card from = activeCard(CARD_A, new BigDecimal("100.00"));
        Card to = activeCard(CARD_B, BigDecimal.ZERO);
        UserAccount other = new UserAccount();
        other.setId(UUID.randomUUID());
        to.setUser(other);

        when(cardRepository.findLockedById(CARD_A)).thenReturn(Optional.of(from));
        when(cardRepository.findLockedById(CARD_B)).thenReturn(Optional.of(to));

        assertThrows(
                ForbiddenException.class,
                () -> cardManagementService.transfer(new TransferRequest(CARD_A, CARD_B, new BigDecimal("1.00"))));
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void transfer_sourceBelongsToAnotherUser_forbidden() {
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        when(currentUserService.isAdmin()).thenReturn(false);

        Card from = activeCard(CARD_A, new BigDecimal("100.00"));
        UserAccount other = new UserAccount();
        other.setId(UUID.randomUUID());
        from.setUser(other);
        Card to = activeCard(CARD_B, BigDecimal.ZERO);

        when(cardRepository.findLockedById(CARD_A)).thenReturn(Optional.of(from));
        when(cardRepository.findLockedById(CARD_B)).thenReturn(Optional.of(to));

        assertThrows(
                ForbiddenException.class,
                () -> cardManagementService.transfer(new TransferRequest(CARD_A, CARD_B, new BigDecimal("1.00"))));
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void transfer_asAdmin_betweenDifferentOwners_updatesBalances() {
        UserAccount adminAccount = new UserAccount();
        adminAccount.setId(UUID.randomUUID());
        adminAccount.setRole(UserRole.ADMIN);
        when(currentUserService.requireCurrentUser()).thenReturn(adminAccount);
        when(currentUserService.isAdmin()).thenReturn(true);

        UserAccount payer = new UserAccount();
        payer.setId(UUID.randomUUID());
        UserAccount payee = new UserAccount();
        payee.setId(UUID.randomUUID());

        Card from = activeCard(CARD_A, new BigDecimal("50.00"));
        from.setUser(payer);
        Card to = activeCard(CARD_B, new BigDecimal("10.00"));
        to.setUser(payee);

        when(cardRepository.findLockedById(CARD_A)).thenReturn(Optional.of(from));
        when(cardRepository.findLockedById(CARD_B)).thenReturn(Optional.of(to));

        cardManagementService.transfer(new TransferRequest(CARD_A, CARD_B, new BigDecimal("25.00")));

        assertEquals(new BigDecimal("25.00"), from.getBalance());
        assertEquals(new BigDecimal("35.00"), to.getBalance());
        verify(cardRepository, times(2)).save(any(Card.class));
    }

    @Test
    void requestBlock_ownActiveCard_setsBlocked() {
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        Card card = activeCard(CARD_A, new BigDecimal("1.00"));
        when(cardRepository.findDetailById(CARD_A)).thenReturn(Optional.of(card));

        CardResponse response = cardManagementService.requestBlock(CARD_A);

        assertEquals(CardStatus.BLOCKED, response.status());
        verify(cardRepository).save(card);
    }

    @Test
    void requestBlock_foreignCard_forbidden() {
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        UserAccount other = new UserAccount();
        other.setId(UUID.randomUUID());
        Card card = activeCard(CARD_A, new BigDecimal("1.00"));
        card.setUser(other);
        when(cardRepository.findDetailById(CARD_A)).thenReturn(Optional.of(card));

        assertThrows(ForbiddenException.class, () -> cardManagementService.requestBlock(CARD_A));
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void requestBlock_expiredCard_badRequest() {
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        Card card = activeCard(CARD_A, new BigDecimal("1.00"));
        card.setStatus(CardStatus.EXPIRED);
        when(cardRepository.findDetailById(CARD_A)).thenReturn(Optional.of(card));

        assertThrows(BadRequestException.class, () -> cardManagementService.requestBlock(CARD_A));
        verify(cardRepository, never()).save(any(Card.class));
    }

    @Test
    void getById_foreignCard_forbidden() {
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        UserAccount other = new UserAccount();
        other.setId(UUID.randomUUID());
        Card card = activeCard(CARD_A, new BigDecimal("1.00"));
        card.setUser(other);
        when(cardRepository.findDetailById(CARD_A)).thenReturn(Optional.of(card));

        assertThrows(ForbiddenException.class, () -> cardManagementService.getById(CARD_A));
    }

    @Test
    void list_asUser_resolvesCurrentUserAndQueriesWithSpecification() {
        when(currentUserService.isAdmin()).thenReturn(false);
        when(currentUserService.requireCurrentUser()).thenReturn(owner);
        Pageable pageable = PageRequest.of(0, 10);
        when(cardRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        var page = cardManagementService.list(pageable, null, null, null);

        assertTrue(page.isEmpty());
        verify(currentUserService).requireCurrentUser();
        verify(cardRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void list_asAdmin_withoutUserFilter_queriesWithoutResolvingCurrentUser() {
        when(currentUserService.isAdmin()).thenReturn(true);
        Pageable pageable = PageRequest.of(0, 10);
        when(cardRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        var page = cardManagementService.list(pageable, CardStatus.ACTIVE, "ivan", null);

        assertTrue(page.isEmpty());
        verify(currentUserService, never()).requireCurrentUser();
        verify(cardRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void list_asAdmin_withUserIdFilter_queriesWithoutRequireCurrentUser() {
        when(currentUserService.isAdmin()).thenReturn(true);
        UUID filterUserId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(1, 5);
        when(cardRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(Collections.emptyList()));

        cardManagementService.list(pageable, null, null, filterUserId);

        verify(currentUserService, never()).requireCurrentUser();
        verify(cardRepository).findAll(any(Specification.class), eq(pageable));
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
