package com.learnhub.auth.controller;

import com.learnhub.auth.service.AuthService;
import com.learnhub.user.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String password = body.get("password");
        User u = authService.register(email, password);
        return ResponseEntity.status(201).body(Map.of("id", u.getId(), "email", u.getEmail()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) throws Exception {
        String email = body.get("email");
        String password = body.get("password");
        String access = authService.login(email, password);
        // Simplified: refresh token same as access for sprint 1 (rotate later)
        String refresh = access;
        return ResponseEntity.ok(Map.of("accessToken", access, "refreshToken", refresh));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) throws Exception {
        String refreshToken = body.get("refreshToken");
        String access = authService.refresh(refreshToken);
        String refresh = access;
        return ResponseEntity.ok(Map.of("accessToken", access, "refreshToken", refresh));
    }
}
