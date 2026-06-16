package com.learnhub.auth.service;

import com.learnhub.user.model.User;
import com.learnhub.user.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public User register(String email, String password) {
        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(passwordEncoder.encode(password));
        return userRepository.save(u);
    }

    public String login(String email, String password) throws Exception {
        Optional<User> ou = userRepository.findByEmail(email);
        if (ou.isEmpty()) throw new IllegalArgumentException("Invalid credentials");
        User u = ou.get();
        if (!passwordEncoder.matches(password, u.getPasswordHash())) throw new IllegalArgumentException("Invalid credentials");
        return jwtService.createAccessToken(u.getId().toString(), u.getRole());
    }

    public String refresh(String refreshToken) throws Exception {
        // Basic validation: verify signature and expiration before issuing new access token
        if (!jwtService.validateToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }
        com.nimbusds.jwt.SignedJWT jwt = com.nimbusds.jwt.SignedJWT.parse(refreshToken);
        String sub = jwt.getJWTClaimsSet().getSubject();
        return jwtService.createAccessToken(sub, "LEARNER");
    }
}
