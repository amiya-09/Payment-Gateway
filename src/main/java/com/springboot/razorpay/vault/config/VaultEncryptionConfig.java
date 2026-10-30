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
    // In here, I'm going to configure my vault encryption like how we want to encrypt our dek and
    // how we want to encrypt our data. We can use AES encryption for this purpose.
    @Value("${vault.master-key}")
    private String masterKey;
    // Now we want to generate a PAN encryptor

    public static BytesEncryptor panEncryptor(byte[] dek) {
        SecretKeySpec decKey = new SecretKeySpec(dek, "AES");
        return new AesBytesEncryptor(decKey, KeyGenerators.secureRandom(12), AesBytesEncryptor.CipherAlgorithm.GCM);
        // It needs an ivGenerator - this is the initialization vector so that no 2 inputs have the same output
        // otherwise what happens in an encryption is that if you encrypt the same input twice, you will get the same output.
        // So we want to make sure that we have a different output for the same input.
        // So we need to use an ivGenerator which will generate a random value for us. So we can use the default ivGenerator provided by Spring Security.
    }

    // This would be a static bean because every dek would be encrypted on the basis of this master key.
    // So it can be a standard value across the application - that's why it is a bean that can be used throughout the application.
    @Bean
    public BytesEncryptor dekEncryptor() {
        byte[] masterKeyBytes = Base64.getDecoder().decode(masterKey);
        SecretKeySpec masterDecKey = new SecretKeySpec(masterKeyBytes, "AES");
        return new AesBytesEncryptor(masterDecKey, KeyGenerators.secureRandom(12), AesBytesEncryptor.CipherAlgorithm.GCM);
    }
}
