package com.springboot.razorpay.operations.entity;

import com.springboot.razorpay.common.entity.BaseEntity;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.util.UUID;

@Embeddable
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode
public class SettlementPaymentId {
    private UUID settlementId;
    private UUID paymentId;
}
