package com.springboot.razorpay.operations.entity;

import com.springboot.razorpay.common.entity.BaseEntity;
import com.springboot.razorpay.common.entity.Money;
import com.springboot.razorpay.common.enums.SettlementStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "settlement")
public class Settlement extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID merchantId;

    @Embedded
    @AttributeOverride(name = "amountUnits", column = @Column(name = "gross_amount_units", nullable = false))
    @AttributeOverride(name = "currency", column = @Column(name = "gross_amount_currency", nullable = false))
    private Money grossAmount;

    @Embedded
    @AttributeOverride(name = "amountUnits", column = @Column(name = "refund_amount_units", nullable = false))
    @AttributeOverride(name = "currency", column = @Column(name = "refund_amount_currency", nullable = false))
    private Money refundAmount;

    @Embedded
    @AttributeOverride(name = "amountUnits", column = @Column(name = "fee_amount_units", nullable = false))
    @AttributeOverride(name = "currency", column = @Column(name = "fee_amount_currency", nullable = false))
    private Money feeAmount;

    @Embedded
    @AttributeOverride(name = "amountUnits", column = @Column(name = "gst_amount_units", nullable = false))
    @AttributeOverride(name = "currency", column = @Column(name = "gst_amount_currency", nullable = false))
    private Money gstAmount;

    @Embedded
    @AttributeOverride(name = "amountUnits", column = @Column(name = "net_amount_units", nullable = false))
    @AttributeOverride(name = "currency", column = @Column(name = "net_amount_currency", nullable = false))
    private Money netAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status;

    @Column(nullable = false, length = 20)
    private String bankReference;

    private LocalDateTime processedAt;
}
