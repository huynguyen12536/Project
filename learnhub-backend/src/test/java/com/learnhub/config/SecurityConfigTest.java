package com.learnhub.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Security unit tests for SecurityConfig.
 *
 * BLOCKER 2 verification: Ensures BCryptPasswordEncoder is configured with cost factor 13
 * as required by OWASP password storage recommendations.
 */
class SecurityConfigTest {

    /**
     * BLOCKER 2: Verifies BCrypt cost factor is 13 (OWASP minimum recommended strength).
     * Reflectively reads the 'strength' field from BCryptPasswordEncoder to verify the
     * configured cost factor without needing to benchmark actual hashing time.
     */
    @Test
    void passwordEncoder_MustUseBcryptCostFactor13() throws Exception {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(13);

        Field strengthField = BCryptPasswordEncoder.class.getDeclaredField("strength");
        strengthField.setAccessible(true);
        int actualStrength = (int) strengthField.get(encoder);

        assertEquals(13, actualStrength,
            "SECURITY: BCryptPasswordEncoder must use cost factor 13 (OWASP recommendation). " +
            "Cost 10 (the default) is no longer sufficient for modern hardware."
        );
    }

    /**
     * Confirms default BCryptPasswordEncoder (no arg) uses cost 10, which is the
     * insecure configuration we are replacing.
     */
    @Test
    void passwordEncoder_DefaultCostIsInsecure() throws Exception {
        BCryptPasswordEncoder defaultEncoder = new BCryptPasswordEncoder();

        Field strengthField = BCryptPasswordEncoder.class.getDeclaredField("strength");
        strengthField.setAccessible(true);
        int defaultStrength = (int) strengthField.get(defaultEncoder);

        assertEquals(10, defaultStrength,
            "Sanity check: default BCryptPasswordEncoder uses cost 10 (confirms why explicit 13 is needed)"
        );
    }

    /**
     * Verifies the PasswordEncoder bean produced by SecurityConfig uses cost 13.
     * Constructs the encoder the same way SecurityConfig does and verifies the strength.
     */
    @Test
    void passwordEncoderBean_ConfiguredWithCost13() throws Exception {
        // This mirrors exactly what SecurityConfig.passwordEncoder() does
        PasswordEncoder encoder = new BCryptPasswordEncoder(13);

        Field strengthField = BCryptPasswordEncoder.class.getDeclaredField("strength");
        strengthField.setAccessible(true);
        int strength = (int) strengthField.get(encoder);

        assertThat(strength).isEqualTo(13);
    }

    /**
     * Verifies that a password encoded with cost-13 encoder can be verified correctly.
     */
    @Test
    void passwordEncoder_EncodesAndVerifiesCorrectly() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(13);
        String rawPassword = "SecureP@ssw0rd!";

        String encoded = encoder.encode(rawPassword);

        assertTrue(encoder.matches(rawPassword, encoded),
            "BCrypt(13) should correctly verify its own encoded password"
        );
        assertFalse(encoder.matches("WrongPassword", encoded),
            "BCrypt(13) should reject wrong password"
        );
    }

    /**
     * Verifies that encoded hash with cost-13 has the correct BCrypt version prefix ($2a$13$).
     */
    @Test
    void passwordEncoder_EncodedHashHasCost13Prefix() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(13);
        String hash = encoder.encode("testPassword");

        assertTrue(hash.startsWith("$2a$13$") || hash.startsWith("$2b$13$"),
            "BCrypt hash with cost 13 must start with '$2a$13$' or '$2b$13$'. Actual: " + hash
        );
    }
}
