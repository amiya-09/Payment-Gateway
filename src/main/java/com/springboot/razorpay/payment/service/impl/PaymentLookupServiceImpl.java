package com.springboot.razorpay.payment.service.impl;

import com.springboot.razorpay.common.enums.PaymentStatus;
import com.springboot.razorpay.payment.api.PaymentLookupService;
import com.springboot.razorpay.payment.entity.Payment;
import com.springboot.razorpay.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentLookupServiceImpl implements PaymentLookupService {

    private final PaymentRepository paymentRepository;

    @Override
    public List<Payment> findUnsettledCapturedPayments(UUID merchantId) {
        return paymentRepository.findByMerchantIdAndStatusForUpdate(merchantId, PaymentStatus.CAPTURED);
    }
}
