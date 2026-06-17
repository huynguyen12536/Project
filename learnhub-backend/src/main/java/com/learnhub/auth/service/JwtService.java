package com.learnhub.auth.service;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${jwt.access-token-ttl-seconds:900}")
    private long accessTokenTtlSeconds;

    @Value("${jwt.refresh-token-ttl-seconds:2592000}")
    private long refreshTokenTtlSeconds;

    private final RsaKeyManager rsaKeyManager;

    public JwtService(RsaKeyManager rsaKeyManager) {
        this.rsaKeyManager = rsaKeyManager;
    }

    public String createAccessToken(String subject, String roles) throws Exception {
        RSAPrivateKey privateKey = rsaKeyManager.getPrivateKey();
        int keyVersion = rsaKeyManager.getKeyVersion();

        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .claim("roles", roles)
                .claim("token_version", keyVersion)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(accessTokenTtlSeconds)))
                .jwtID(UUID.randomUUID().toString())
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(String.valueOf(keyVersion))
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claims);
        RSASSASigner signer = new RSASSASigner(privateKey);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    public String createRefreshToken(String subject) throws Exception {
        RSAPrivateKey privateKey = rsaKeyManager.getPrivateKey();
        int keyVersion = rsaKeyManager.getKeyVersion();

        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .claim("token_type","refresh")
                .claim("token_version", keyVersion)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(refreshTokenTtlSeconds)))
                .jwtID(UUID.randomUUID().toString())
                .build();

        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256)
                .keyID(String.valueOf(keyVersion))
                .build();

        SignedJWT signedJWT = new SignedJWT(header, claims);
        RSASSASigner signer = new RSASSASigner(privateKey);
        signedJWT.sign(signer);
        return signedJWT.serialize();
    }

    public boolean validateToken(String token) {
        try {
            SignedJWT jwt = SignedJWT.parse(token);
            int kid = Integer.parseInt(jwt.getHeader().getKeyID());
            RSAPublicKey pub = rsaKeyManager.getPublicKeyForVersion(kid);
            boolean sigValid = jwt.verify(new com.nimbusds.jose.crypto.RSASSAVerifier(pub));
            if (!sigValid) return false;
            Date exp = jwt.getJWTClaimsSet().getExpirationTime();
            if (exp == null) return false;
            if (exp.before(new Date())) return false;
            // token_version check could be implemented here; for now ensure kid exists
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

}
