package com.springboot.razorpay.vault.service.impl;

import com.springboot.razorpay.common.entity.Money;
import com.springboot.razorpay.common.enums.CardBrand;
import com.springboot.razorpay.common.exception.ResourceNotFoundException;
import com.springboot.razorpay.common.util.RandomizerUtil;
import com.springboot.razorpay.payment.processor.PaymentProcessor;
import com.springboot.razorpay.payment.processor.PaymentProcessorRouter;
import com.springboot.razorpay.payment.processor.dto.PaymentProcessorRequest;
import com.springboot.razorpay.payment.processor.dto.PaymentProcessorResponse;
import com.springboot.razorpay.vault.config.VaultEncryptionConfig;
import com.springboot.razorpay.vault.dto.request.TokenizeRequest;
import com.springboot.razorpay.vault.dto.response.TokenizeResponse;
import com.springboot.razorpay.vault.entity.CardToken;
import com.springboot.razorpay.vault.entity.VaultCard;
import com.springboot.razorpay.vault.repository.CardTokenRepository;
import com.springboot.razorpay.vault.repository.VaultCardRepository;
import com.springboot.razorpay.vault.service.VaultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.security.crypto.keygen.KeyGenerators;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class VaultServiceImpl implements VaultService {

    private final VaultCardRepository vaultCardRepository;
    private final CardTokenRepository cardTokenRepository;
    private final BytesEncryptor dekEncryptor;
    private final PaymentProcessorRouter paymentProcessorRouter;

    @Override
    @Transactional
    public TokenizeResponse tokenize(TokenizeRequest request, UUID merchantId) {
        String lastFour = request.pan().substring(request.pan().length() - 4);
        String bin = request.pan().substring(0, 6);
        CardBrand cardBrand = detectBrand(request.pan());
        // Encrypt PAN with a fresh data-encryption key (DEK).
        byte[] dek = KeyGenerators.secureRandom(32).generateKey(); // This generated a 32 byte long dek
        byte[] encryptedPan = VaultEncryptionConfig.panEncryptor(dek).encrypt(request.pan().getBytes(StandardCharsets.UTF_8));
        // PAN is encrypted with DEK, but DEK must also be stored safely for future decryption.
        // Storing raw DEK is unsafe, so encrypt DEK with the master-key encryptor first.
        // Persist only encrypted DEK and encrypted PAN.
        byte[] encryptedDek = dekEncryptor.encrypt(dek);
        VaultCard vaultCard = vaultCardRepository.save(VaultCard.builder()
                .brand(cardBrand)
                .expiryYear(request.expiryYear().toString())
                .expiryMonth(request.expiryMonth().toString())
                .bin(bin)
                .lastFour(lastFour)
                .encryptedDek(encryptedDek)
                .encryptedPan(encryptedPan)
                .cardHolderName(request.cardHolderName())
                .build());

        String token = "tok_"+ RandomizerUtil.randomBase64(32);
        cardTokenRepository.save(CardToken.builder()
                        .vaultCard(vaultCard)
                        .token(token)
                        .customer(request.customerId())
                        .merchant(merchantId)
                .build());
        return new TokenizeResponse(token, lastFour, cardBrand, request.expiryMonth(), request.expiryYear());
    }

    @Override
    public PaymentProcessorResponse charge(UUID paymentId, String token, Money amount, Map<String, Object> methodDetails) {
        // Resolve token, then decrypt card data needed by the processor.
        CardToken cardToken = cardTokenRepository.findByTokenAndRevokedAtIsNull(token)
                .orElseThrow(() -> new ResourceNotFoundException("CardToken", token));
        // Token points to a vault card record containing encrypted values.
        VaultCard vaultCard = cardToken.getVaultCard();
        byte[] panBytes = null;
        // Decrypt stored encrypted DEK to get usable DEK for PAN decryption.
        try {
            byte[] dek = dekEncryptor.decrypt(vaultCard.getEncryptedDek());
            panBytes = VaultEncryptionConfig.panEncryptor(dek).decrypt(vaultCard.getEncryptedPan());
            String pan = new String(panBytes, StandardCharsets.UTF_8); // this is the pan that the banks, card networks actually understand
            String expiry = vaultCard.getExpiryMonth() + "/" + vaultCard.getExpiryYear();
            // Vault service invokes processor here because it holds decrypted card details.
            // Adapter does not decrypt PAN; it only forwards payment intent.
            // Build processor request, call downstream network, and return the response.
            PaymentProcessorRequest paymentProcessorRequest = PaymentProcessorRequest.card(paymentId, pan, expiry, amount, methodDetails);
            PaymentProcessorResponse response = paymentProcessorRouter.charge(paymentProcessorRequest);
            log.info("Vault charge registered, token={}****", token.substring(0, 4));
            return response;
        } catch (Exception e) {
            log.warn("Vault charge failed, token={}****", token.substring(0, 4));
            return new PaymentProcessorResponse.Failure("VAULT_CHARGE_FAILED", "Vault charge failed: " + e.getMessage());
        } finally {
            if(panBytes != null) Arrays.fill(panBytes, (byte) 0);
        }
    }

    private CardBrand detectBrand(String pan) {
        if(pan.startsWith("4")) {
            return CardBrand.VISA;
        } else if(pan.startsWith("5") || pan.startsWith("2")) {
            return CardBrand.MASTERCARD;
        } else if(pan.startsWith("37") ||  pan.startsWith("34")) {
            return CardBrand.AMEX;
        } else {
            return CardBrand.RUPAY;
        }
    }
}
