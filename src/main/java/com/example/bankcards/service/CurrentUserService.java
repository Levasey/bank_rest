package com.example.bankcards.service;

import com.example.bankcards.entity.UserAccount;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.UserAccountRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {

    private final UserAccountRepository userAccountRepository;

    public CurrentUserService(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    public UserAccount requireCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null
                || !auth.isAuthenticated()
                || "anonymousUser".equalsIgnoreCase(auth.getName())) {
            throw new NotFoundException("Not authenticated");
        }
        String username = auth.getName();
        return userAccountRepository
                .findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    public boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
