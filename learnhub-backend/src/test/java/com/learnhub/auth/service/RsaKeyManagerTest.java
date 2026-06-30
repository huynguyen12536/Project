package com.learnhub.auth.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Security unit tests for RsaKeyManager.
 *
 * BLOCKER 4 verification: Ensures that RsaKeyManager fails fast (throws exception)
 * when the RSA private key is absent, rather than silently generating an ephemeral
 * in-memory key that would invalidate all JWTs on application restart.
 */
class RsaKeyManagerTest {

    /**
     * BLOCKER 4 (core): Application must fail to start when JWT_RSA_PRIVATE_KEY is absent.
     * The old code silently generated an in-memory key — this fix removes that fallback.
     */
    @Test
    void init_MustThrow_WhenPrivateKeyIsAbsent() {
        RsaKeyManager manager = new RsaKeyManager();
        ReflectionTestUtils.setField(manager, "privateKeyPem", "");
        ReflectionTestUtils.setField(manager, "publicKeyPem", "");

        RuntimeException ex = assertThrows(
            RuntimeException.class,
            manager::init,
            "SECURITY: RsaKeyManager must throw if jwt.rsa.private-key-file is absent. " +
            "Ephemeral in-memory key generation is forbidden as it invalidates all JWTs on restart."
        );

        // Root cause must be IllegalStateException with descriptive FATAL message
        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        assertThat(cause).isInstanceOf(IllegalStateException.class);
        assertThat(cause.getMessage()).contains("FATAL");
        assertThat(cause.getMessage()).containsIgnoringCase("private-key");
    }

    /**
     * BLOCKER 4: The error message must include actionable guidance (AWS Secrets Manager mention).
     */
    @Test
    void init_ErrorMessage_MustReferenceSecretStore() {
        RsaKeyManager manager = new RsaKeyManager();
        ReflectionTestUtils.setField(manager, "privateKeyPem", null);
        ReflectionTestUtils.setField(manager, "publicKeyPem", null);

        RuntimeException ex = assertThrows(RuntimeException.class, manager::init);

        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        String message = cause.getMessage();

        assertThat(message).satisfiesAnyOf(
            msg -> assertThat(msg).containsIgnoringCase("aws secrets manager"),
            msg -> assertThat(msg).containsIgnoringCase("secret store"),
            msg -> assertThat(msg).containsIgnoringCase("secrets manager")
        );
    }

    /**
     * BLOCKER 4: Application must fail to start when public key is absent (even if private key present).
     */
    @Test
    void init_MustThrow_WhenPublicKeyIsAbsent() {
        // We supply a private key PEM but no public key
        String fakePem = "-----BEGIN PRIVATE KEY-----\nZmFrZWtleQ==\n-----END PRIVATE KEY-----";
        RsaKeyManager manager = new RsaKeyManager();
        ReflectionTestUtils.setField(manager, "privateKeyPem", fakePem);
        ReflectionTestUtils.setField(manager, "publicKeyPem", "");

        RuntimeException ex = assertThrows(
            RuntimeException.class,
            manager::init,
            "SECURITY: RsaKeyManager must throw when public key is absent."
        );

        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        assertThat(cause).isInstanceOf(IllegalStateException.class);
        assertThat(cause.getMessage()).contains("FATAL");
        assertThat(cause.getMessage()).containsIgnoringCase("public-key");
    }

    /**
     * BLOCKER 4: Verifies that null private key (as Spring injects when env var absent) also fails fast.
     * Spring injects empty string for @Value("${...}:") default, but null for missing @Value.
     */
    @Test
    void init_MustThrow_WhenPrivateKeyIsNull() {
        RsaKeyManager manager = new RsaKeyManager();
        ReflectionTestUtils.setField(manager, "privateKeyPem", null);
        ReflectionTestUtils.setField(manager, "publicKeyPem", "some-key");

        RuntimeException ex = assertThrows(
            RuntimeException.class,
            manager::init
        );

        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        assertThat(cause).isInstanceOf(IllegalStateException.class);
    }

    /**
     * Verifies blank (whitespace-only) private key also triggers fail-fast.
     */
    @Test
    void init_MustThrow_WhenPrivateKeyIsBlank() {
        RsaKeyManager manager = new RsaKeyManager();
        ReflectionTestUtils.setField(manager, "privateKeyPem", "   ");
        ReflectionTestUtils.setField(manager, "publicKeyPem", "   ");

        RuntimeException ex = assertThrows(
            RuntimeException.class,
            manager::init,
            "Blank private key must also trigger fail-fast."
        );

        Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
        assertThat(cause).isInstanceOf(IllegalStateException.class);
    }
}
