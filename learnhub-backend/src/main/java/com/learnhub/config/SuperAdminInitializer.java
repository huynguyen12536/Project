package com.learnhub.config;

import com.learnhub.user.model.User;
import com.learnhub.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class SuperAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.super-admin.enabled:true}")
    private boolean enabled;

    @Value("${app.super-admin.email:}")
    private String email;

    @Value("${app.super-admin.password:}")
    private String password;

    @Value("${app.super-admin.first-name:System}")
    private String firstName;

    @Value("${app.super-admin.last-name:Admin}")
    private String lastName;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Super admin seed disabled");
            return;
        }

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            log.warn("Super admin seed skipped because email or password is missing");
            return;
        }

        userRepository.findByEmail(email)
            .ifPresentOrElse(this::promoteExistingUser, this::createSuperAdmin);
    }

    private void promoteExistingUser(User user) {
        boolean changed = false;

        if (!"ADMIN".equals(user.getRole())) {
            user.setRole("ADMIN");
            changed = true;
        }
        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            user.setEmailVerifiedAt(LocalDateTime.now());
            changed = true;
        }
        if (password != null && !password.isBlank()) {
            user.setPasswordHash(passwordEncoder.encode(password));
            changed = true;
        }
        if (firstName != null && !firstName.isBlank()) {
            user.setFirstName(firstName);
            changed = true;
        }
        if (lastName != null && !lastName.isBlank()) {
            user.setLastName(lastName);
            changed = true;
        }

        if (changed) {
            userRepository.save(user);
            log.info("Super admin account updated: userId={}", user.getId());
        } else {
            log.info("Super admin account already up to date: userId={}", user.getId());
        }
    }

    private void createSuperAdmin() {
        User user = new User();
        user.setEmail(email);
        user.setUsername(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole("ADMIN");
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(LocalDateTime.now());

        User saved = userRepository.save(user);
        log.info("Super admin account created: userId={}", saved.getId());
    }
}
