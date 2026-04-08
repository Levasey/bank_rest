package com.example.bankcards.service;

import com.example.bankcards.dto.UserCreateRequest;
import com.example.bankcards.dto.UserResponse;
import com.example.bankcards.dto.UserUpdateRequest;
import com.example.bankcards.entity.UserAccount;
import com.example.bankcards.exception.BadRequestException;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.UserAccountRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserManagementService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementService(
            UserAccountRepository userAccountRepository, PasswordEncoder passwordEncoder) {
        this.userAccountRepository = userAccountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> list(Pageable pageable) {
        return userAccountRepository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public UserResponse get(UUID id) {
        return userAccountRepository.findById(id).map(this::toResponse).orElseThrow(this::notFound);
    }

    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userAccountRepository.existsByUsername(request.username())) {
            throw new BadRequestException("Username already exists");
        }
        if (userAccountRepository.existsByEmail(request.email())) {
            throw new BadRequestException("Email already exists");
        }
        UserAccount user = new UserAccount();
        user.setId(UUID.randomUUID());
        user.setUsername(request.username().trim());
        user.setEmail(request.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setEnabled(true);
        user.setCreatedAt(Instant.now());
        return toResponse(userAccountRepository.save(user));
    }

    @Transactional
    public UserResponse update(UUID id, UserUpdateRequest request) {
        UserAccount user =
                userAccountRepository.findById(id).orElseThrow(this::notFound);
        if (request.email() != null) {
            String email = request.email().trim().toLowerCase();
            if (!email.equals(user.getEmail()) && userAccountRepository.existsByEmail(email)) {
                throw new BadRequestException("Email already exists");
            }
            user.setEmail(email);
        }
        if (request.password() != null && !request.password().isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }
        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }
        if (request.role() != null) {
            user.setRole(request.role());
        }
        return toResponse(userAccountRepository.save(user));
    }

    @Transactional
    public void delete(UUID id) {
        if (!userAccountRepository.existsById(id)) {
            throw notFound();
        }
        userAccountRepository.deleteById(id);
    }

    private UserResponse toResponse(UserAccount u) {
        return new UserResponse(
                u.getId(), u.getUsername(), u.getEmail(), u.getRole(), u.isEnabled(), u.getCreatedAt());
    }

    private NotFoundException notFound() {
        return new NotFoundException("User not found");
    }
}
