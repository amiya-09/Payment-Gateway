package com.springboot.razorpay.merchant.repository;

import com.springboot.razorpay.merchant.entity.MerchantWebhookConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebhookConfigRepository extends JpaRepository<MerchantWebhookConfig, UUID> {
    Optional<MerchantWebhookConfig> findByIdAndMerchant_Id(UUID configId, UUID merchantId);

    List<MerchantWebhookConfig> findByMerchant_Id(UUID merchantId);

    List<MerchantWebhookConfig> findByMerchant_IdAndEnabledTrue(UUID merchantId);
}
