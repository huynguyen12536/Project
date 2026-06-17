package com.learnhub.auth.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class TokenHashService {
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hashToken(String token) {
        return encoder.encode(token);
    }

    public boolean verifyToken(String token, String hash) {
        return encoder.matches(token, hash);
    }
}
