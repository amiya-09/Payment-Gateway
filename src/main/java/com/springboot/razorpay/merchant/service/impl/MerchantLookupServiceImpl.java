package com.springboot.razorpay.merchant.service.impl;

import com.springboot.razorpay.common.dto.SettlementBankDetails;
import com.springboot.razorpay.common.dto.WebhookTarget;
import com.springboot.razorpay.common.enums.MerchantStatus;
import com.springboot.razorpay.common.exception.ResourceNotFoundException;
import com.springboot.razorpay.merchant.api.MerchantLookupService;
import com.springboot.razorpay.merchant.entity.Merchant;
import com.springboot.razorpay.merchant.repository.MerchantRepository;
import com.springboot.razorpay.merchant.repository.WebhookConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.encrypt.BytesEncryptor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
@Slf4j
public class MerchantLookupServiceImpl implements MerchantLookupService {

    private final WebhookConfigRepository webhookConfigRepository;
    private final MerchantRepository merchantRepository;
    private final BytesEncryptor bytesEncryptor;

    @Override
    public List<WebhookTarget> getActiveConfigForEvent(UUID merchantId, String eventType) {
        // This method should return the list of active webhook configurations for the given merchant and event type.
        // We will query the database for all active webhook configurations for the given merchant and event type.
        return webhookConfigRepository.findByMerchant_IdAndEnabledTrue(merchantId).stream()
                .filter( config -> config.isSubscribedToEvent(eventType))
                .map(config -> {
                    byte[] cipherBytes = Base64.getDecoder().decode(config.getWebhookSecret());
                    byte[] decryptedSecretBytes = bytesEncryptor.decrypt(cipherBytes);
                    return new WebhookTarget(config.getId(), config.getTargetUrl(),
                            new String(decryptedSecretBytes, StandardCharsets.UTF_8));
                })
                .toList();
    }

    @Override
    public List<UUID> listActiveMerchantIds() {
        // This method should return the list of active merchant IDs.
        // We will query the database for all active merchant IDs.
        return merchantRepository.findByStatus(MerchantStatus.ACTIVE)
                .stream().map(m -> m.getId()).toList();
    }

    @Override
    public SettlementBankDetails getSettlementBankDetails(UUID merchantId) {
        // This method should return the settlement bank details for the given merchant ID.
        // We will query the database for the settlement bank details for the given merchant ID.
        Merchant merchant = merchantRepository.findById(merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Merchant",  merchantId));

        return new  SettlementBankDetails(
                merchant.getSettlementBankAccount(),
                merchant.getSettlementBankIfsc(),
                merchant.getSettlementBankAccountHolderName()
        );
    }
}
