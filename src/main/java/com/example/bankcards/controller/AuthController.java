package com.example.bankcards.controller;

import com.example.bankcards.config.AppSecurityProperties;
import com.example.bankcards.dto.LoginRequest;
import com.example.bankcards.dto.TokenResponse;
import com.example.bankcards.entity.UserAccount;
import com.example.bankcards.exception.NotFoundException;
import com.example.bankcards.repository.UserAccountRepository;
import com.example.bankcards.security.JwtTokenProvider;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserAccountRepository userAccountRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final AppSecurityProperties appSecurityProperties;

    public AuthController(
            AuthenticationManager authenticationManager,
            UserAccountRepository userAccountRepository,
            JwtTokenProvider jwtTokenProvider,
            AppSecurityProperties appSecurityProperties) {
        this.authenticationManager = authenticationManager;
        this.userAccountRepository = userAccountRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.appSecurityProperties = appSecurityProperties;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.username(), request.password()));
        UserAccount user =
                userAccountRepository
                        .findByUsername(request.username())
                        .orElseThrow(() -> new NotFoundException("User not found"));
        String token = jwtTokenProvider.createToken(user);
        long expiresSec = appSecurityProperties.getJwt().getExpirationMs() / 1000;
        return ResponseEntity.ok(new TokenResponse(token, "Bearer", expiresSec));
    }
}
