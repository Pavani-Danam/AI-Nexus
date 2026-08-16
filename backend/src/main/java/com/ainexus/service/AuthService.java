package com.ainexus.service;

import com.ainexus.dto.AuthResponse;
import com.ainexus.dto.LoginRequest;
import com.ainexus.dto.LoginResponse;
import com.ainexus.dto.LogoutRequest;
import com.ainexus.dto.RefreshTokenRequest;
import com.ainexus.entity.RefreshToken;
import com.ainexus.entity.User;
import com.ainexus.repository.UserRepository;
import com.ainexus.security.JwtService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RefreshTokenService refreshTokenService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenService = refreshTokenService;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        if (!user.isEnabled()) {
            throw new BadCredentialsException("User account is disabled");
        }

        String accessToken = jwtService.generateToken(user.getEmail());
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        AuthResponse authUser = AuthResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationSeconds())
                .user(authUser)
                .build();
    }

    public LoginResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenService.findByToken(request.getRefreshToken());
        refreshTokenService.verifyExpirationAndRevocation(token);

        User user = token.getUser();
        String newAccessToken = jwtService.generateToken(user.getEmail());
        RefreshToken rotatedRefreshToken = refreshTokenService.createRefreshToken(user.getEmail());

        AuthResponse authUser = AuthResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(rotatedRefreshToken.getToken())
                .tokenType("Bearer")
                .expiresIn(jwtService.getExpirationSeconds())
                .user(authUser)
                .build();
    }

    public void logout(LogoutRequest request) {
        refreshTokenService.revokeToken(request.getRefreshToken());
    }
}
