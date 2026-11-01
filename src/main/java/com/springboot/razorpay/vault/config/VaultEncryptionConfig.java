package com.springboot.razorpay.vault.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.encrypt.AesBytesEncryptor;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.security.crypto.keygen.KeyGenerators;

import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class VaultEncryptionConfig {
    // Configure vault encryption for both PAN data and DEK wrapping, using AES-based encryptors.
    @Value("${vault.master-key}")
    private String masterKey;
    // Build encryptor used for PAN encryption/decryption with a per-card DEK.

    public static BytesEncryptor panEncryptor(byte[] dek) {
        SecretKeySpec decKey = new SecretKeySpec(dek, "AES");
        return new AesBytesEncryptor(decKey, KeyGenerators.secureRandom(12), AesBytesEncryptor.CipherAlgorithm.GCM);
        // Use a random IV so encrypting identical plaintext does not produce identical ciphertext.
        // This prevents deterministic outputs for repeated PAN values.
    }

    // Shared bean that encrypts/decrypts DEKs using a single configured master key.
    // It is application-wide because all DEKs are wrapped with the same master key.
    @Bean
    public BytesEncryptor dekEncryptor() {
        byte[] masterKeyBytes = Base64.getDecoder().decode(masterKey);
        SecretKeySpec masterDecKey = new SecretKeySpec(masterKeyBytes, "AES");
        return new AesBytesEncryptor(masterDecKey, KeyGenerators.secureRandom(12), AesBytesEncryptor.CipherAlgorithm.GCM);
    }
}
