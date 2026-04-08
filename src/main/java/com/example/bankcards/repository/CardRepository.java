package com.example.bankcards.repository;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.UserAccount;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, UUID>, JpaSpecificationExecutor<Card> {

    long countByUser(UserAccount user);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Card c JOIN FETCH c.user WHERE c.id = :id")
    Optional<Card> findLockedById(@Param("id") UUID id);

    @Query("SELECT c FROM Card c JOIN FETCH c.user WHERE c.id = :id")
    Optional<Card> findDetailById(@Param("id") UUID id);
}
