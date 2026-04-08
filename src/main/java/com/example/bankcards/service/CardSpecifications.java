package com.example.bankcards.service;

import com.example.bankcards.entity.Card;
import com.example.bankcards.entity.CardStatus;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class CardSpecifications {

    private CardSpecifications() {
    }

    public static Specification<Card> filtered(UUID ownerId, CardStatus status, String search) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (ownerId != null) {
                predicates.add(cb.equal(root.get("user").get("id"), ownerId));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (search != null && !search.isBlank()) {
                String trimmed = search.trim();
                String holderPattern = "%" + trimmed.toLowerCase() + "%";
                Predicate holderMatch = cb.like(cb.lower(root.get("holderName")), holderPattern);
                String digits = trimmed.replaceAll("\\D", "");
                Predicate searchPredicate = holderMatch;
                if (digits.length() >= 4) {
                    String last4 = digits.substring(digits.length() - 4);
                    Predicate lastFourMatch = cb.equal(root.get("lastFour"), last4);
                    searchPredicate = cb.or(holderMatch, lastFourMatch);
                }
                predicates.add(searchPredicate);
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
