package com.learnhub.auth.service;

import com.learnhub.auth.model.RefreshToken;
import com.learnhub.auth.repository.RefreshTokenRepository;
import com.learnhub.user.model.User;
import com.learnhub.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHashService tokenHashService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService,
                       RefreshTokenRepository refreshTokenRepository, TokenHashService tokenHashService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHashService = tokenHashService;
    }

    public User register(String email, String password) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(password));
        return userRepository.save(u);
    }

    public java.util.Map<String, String> login(String email, String password) throws Exception {
        Optional<User> ou = userRepository.findByEmail(email);
        if (ou.isEmpty()) throw new IllegalArgumentException("Invalid credentials");
        User u = ou.get();
        if (!passwordEncoder.matches(password, u.getPasswordHash())) throw new IllegalArgumentException("Invalid credentials");

        String accessToken = jwtService.createAccessToken(u.getId().toString(), u.getRole());
        String refreshToken = jwtService.createRefreshToken(u.getId().toString());

        String tokenHash = tokenHashService.hashToken(refreshToken);
        RefreshToken rt = new RefreshToken();
        rt.setUserId(u.getId());
        rt.setTokenHash(tokenHash);
        rt.setIssuedAt(Instant.now());
        rt.setExpiresAt(Instant.now().plusSeconds(2592000));
        rt.setRevoked(false);
        refreshTokenRepository.save(rt);

        return java.util.Map.of("accessToken", accessToken, "refreshToken", refreshToken);
    }

    public String refresh(String refreshToken) throws Exception {
        if (!jwtService.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        com.nimbusds.jwt.SignedJWT jwt = com.nimbusds.jwt.SignedJWT.parse(refreshToken);
        String userId = jwt.getJWTClaimsSet().getSubject();

        Optional<RefreshToken> rtOpt = refreshTokenRepository.findByUserIdAndRevokedFalse(UUID.fromString(userId));
        if (rtOpt.isEmpty()) {
            throw new IllegalArgumentException("Refresh token not found or revoked");
        }

        return jwtService.createAccessToken(userId, "LEARNER");
    }

    public void logout(String refreshToken) throws Exception {
        if (!jwtService.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid refresh token");
        }

        com.nimbusds.jwt.SignedJWT jwt = com.nimbusds.jwt.SignedJWT.parse(refreshToken);
        String userId = jwt.getJWTClaimsSet().getSubject();

        Optional<RefreshToken> rtOpt = refreshTokenRepository.findByUserIdAndRevokedFalse(UUID.fromString(userId));
        if (rtOpt.isPresent()) {
            RefreshToken rt = rtOpt.get();
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        }
    }
}
