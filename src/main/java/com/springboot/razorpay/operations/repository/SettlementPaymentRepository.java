package com.springboot.razorpay.operations.repository;

import com.springboot.razorpay.operations.entity.SettlementPayment;
import com.springboot.razorpay.operations.entity.SettlementPaymentId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettlementPaymentRepository extends JpaRepository<SettlementPayment, SettlementPaymentId> {
}
