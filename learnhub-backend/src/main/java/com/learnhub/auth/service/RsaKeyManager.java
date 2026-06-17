package com.learnhub.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

import java.io.StringReader;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
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
        if (privateKeyPem != null && !privateKeyPem.isBlank() && publicKeyPem != null && !publicKeyPem.isBlank()) {
            this.privateKey = (RSAPrivateKey) readPrivateKeyFromPem(privateKeyPem);
            this.publicKey = (RSAPublicKey) readPublicKeyFromPem(publicKeyPem);
        } else {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
            KeyPair kp = kpg.generateKeyPair();
            this.privateKey = (RSAPrivateKey) kp.getPrivate();
            this.publicKey = (RSAPublicKey) kp.getPublic();
        }
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
