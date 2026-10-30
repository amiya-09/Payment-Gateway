package com.springboot.razorpay.payment.service;

import com.springboot.razorpay.payment.dto.request.PaymentInitRequest;
import com.springboot.razorpay.payment.dto.response.PaymentResponse;

import java.util.UUID;

public interface PaymentService {
    PaymentResponse initiate(UUID merchantId, PaymentInitRequest request);

    PaymentResponse capture(UUID merchantId, UUID paymentId);
}
