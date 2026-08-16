package com.ainexus.service;

import com.ainexus.entity.RefreshToken;
import com.ainexus.entity.User;
import com.ainexus.repository.RefreshTokenRepository;
import com.ainexus.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@Transactional
public class RefreshTokenService {

    @Value("${jwt.refresh-expiration-seconds:604800}")
    private long refreshExpirationSeconds;

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, UserRepository userRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    public RefreshToken createRefreshToken(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new BadCredentialsException("User not found for refresh token"));

        String newTokenString = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        Instant newExpiry = Instant.now().plusSeconds(refreshExpirationSeconds);

        RefreshToken refreshToken = refreshTokenRepository.findByUser(user)
                .orElseGet(() -> RefreshToken.builder().user(user).build());

        refreshToken.setToken(newTokenString);
        refreshToken.setExpiryDate(newExpiry);
        refreshToken.setRevoked(false);
        refreshToken.setCreatedAt(Instant.now());

        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyExpirationAndRevocation(RefreshToken token) {
        if (token.isRevoked() || token.getExpiryDate().isBefore(Instant.now())) {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
            throw new BadCredentialsException("Refresh token was expired or revoked. Please log in again.");
        }
        return token;
    }

    public RefreshToken findByToken(String token) {
        return refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));
    }

    public void revokeToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
        });
    }

    public void deleteByUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }
}
