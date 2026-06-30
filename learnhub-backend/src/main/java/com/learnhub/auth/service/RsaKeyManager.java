package com.learnhub.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

@Service
public class RsaKeyManager {

    @Value("${jwt.rsa.private-key-file:}")
    private String privateKeyPem;

    @Value("${jwt.rsa.public-key-file:}")
    private String publicKeyPem;

    private RSAPrivateKey privateKey;
    private RSAPublicKey publicKey;
    private int keyVersion = 1;

    public RsaKeyManager() {
        // no-op constructor; initialization in @PostConstruct
    }

    @PostConstruct
    private void init() {
        try {
            loadKeys();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to initialize RSA keys", ex);
        }
    }

    private void loadKeys() throws Exception {
        // Production must use external key management (e.g., AWS Secrets Manager / Vault).
        // Fail-fast here prevents silent in-memory key generation which invalidates all JWTs on restart.
        if (privateKeyPem == null || privateKeyPem.isBlank()) {
            throw new IllegalStateException(
                "FATAL: jwt.rsa.private-key-file is not configured. " +
                "Configure the RSA private key via AWS Secrets Manager or equivalent secret store. " +
                "In-memory key generation is NOT permitted as it invalidates all JWTs on restart.");
        }
        if (publicKeyPem == null || publicKeyPem.isBlank()) {
            throw new IllegalStateException(
                "FATAL: jwt.rsa.public-key-file is not configured. " +
                "Configure the RSA public key via AWS Secrets Manager or equivalent secret store.");
        }
        this.privateKey = (RSAPrivateKey) readPrivateKeyFromPem(resolvePem(privateKeyPem));
        this.publicKey = (RSAPublicKey) readPublicKeyFromPem(resolvePem(publicKeyPem));
    }

    private String resolvePem(String configuredValue) throws Exception {
        Path path = Path.of(configuredValue);
        if (Files.isRegularFile(path)) {
            return Files.readString(path);
        }
        return configuredValue;
    }

    private RSAPrivateKey readPrivateKeyFromPem(String pem) throws Exception {
        String normalized = pem.replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\n", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPrivateKey) kf.generatePrivate(spec);
    }

    private RSAPublicKey readPublicKeyFromPem(String pem) throws Exception {
        String normalized = pem.replaceAll("-----BEGIN (.*)-----", "")
                .replaceAll("-----END (.*)-----", "")
                .replaceAll("\n", "");
        byte[] decoded = Base64.getDecoder().decode(normalized);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return (RSAPublicKey) kf.generatePublic(spec);
    }

    public RSAPrivateKey getPrivateKey() {
        return privateKey;
    }

    public RSAPublicKey getPublicKeyForVersion(int v) {
        return publicKey; // single key for MVP
    }

    public int getKeyVersion() {
        return keyVersion;
    }
}
