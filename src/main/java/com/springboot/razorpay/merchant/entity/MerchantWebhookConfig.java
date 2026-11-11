package com.springboot.razorpay.merchant.entity;

import com.springboot.razorpay.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "merchant_webhook_config", indexes = {
        @Index(name = "idx_webhook_merchant_id", columnList = "merchant_id, enabled")
})
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchantWebhookConfig extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "merchant_id", nullable = false)
    private Merchant merchant;

    @Column(name = "target_url", nullable = false, length = 500)
    private String targetUrl;

    @Column(name = "webhook_secret_hash", length = 255)
    private String webhookSecret;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(length = 500)
    private String eventTypes;

    public boolean isSubscribedToEvent(String eventType) {
        if (eventTypes == null || eventTypes.isBlank()) {
            return true;
        }
        String[] events = eventTypes.split(",");
        for (String event : events) {
            if (event.trim().equalsIgnoreCase(eventType) || event.trim().equalsIgnoreCase("ALL")) {
                return true;
            }
        }
        return false;
    }
}
