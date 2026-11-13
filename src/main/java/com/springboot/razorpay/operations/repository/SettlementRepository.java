package com.springboot.razorpay.operations.repository;

import com.springboot.razorpay.common.enums.SettlementStatus;
import com.springboot.razorpay.operations.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SettlementRepository extends JpaRepository<Settlement, UUID> {
    List<Settlement> findByStatus(SettlementStatus settlementStatus);
}
