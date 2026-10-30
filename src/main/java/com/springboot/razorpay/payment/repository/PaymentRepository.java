package com.springboot.razorpay.payment.repository;

import com.springboot.razorpay.common.enums.PaymentStatus;
import com.springboot.razorpay.payment.entity.OrderRecord;
import com.springboot.razorpay.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    List<Payment> findByOrder_Id(OrderRecord order);

    Optional<Payment> findByIdAndMerchantId(UUID paymentId, UUID merchantId);

    List<Payment> findByStatusAndCreatedAtBefore(PaymentStatus paymentStatus, LocalDateTime globalWindow);
}
