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
public class InitialDataSeeder implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seedEnabled) {
            log.info("Initial data seed disabled");
            return;
        }

        seedStudentUser();
        seedInstructorUser();
    }

    private void seedStudentUser() {
        String email = "student@learnhub.com";
        String password = "Password123!";
        
        if (userRepository.findByEmail(email).isEmpty()) {
            User student = new User();
            student.setEmail(email);
            student.setUsername(email);
            student.setPasswordHash(passwordEncoder.encode(password));
            student.setFirstName("Nguyen");
            student.setLastName("Van A");
            student.setRole("LEARNER");
            student.setEmailVerified(true);
            student.setEmailVerifiedAt(LocalDateTime.now());
            
            userRepository.save(student);
            log.info("Student user created: email={}", email);
        } else {
            log.info("Student user already exists: email={}", email);
        }
    }

    private void seedInstructorUser() {
        String email = "instructor@learnhub.com";
        String password = "Password123!";
        
        if (userRepository.findByEmail(email).isEmpty()) {
            User instructor = new User();
            instructor.setEmail(email);
            instructor.setUsername(email);
            instructor.setPasswordHash(passwordEncoder.encode(password));
            instructor.setFirstName("Tran");
            instructor.setLastName("Thi B");
            instructor.setRole("INSTRUCTOR");
            instructor.setEmailVerified(true);
            instructor.setEmailVerifiedAt(LocalDateTime.now());
            
            userRepository.save(instructor);
            log.info("Instructor user created: email={}", email);
        } else {
            log.info("Instructor user already exists: email={}", email);
        }
    }
}
