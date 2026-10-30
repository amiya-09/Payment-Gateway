package com.springboot.razorpay.vault.service;

import com.springboot.razorpay.common.entity.Money;
import com.springboot.razorpay.payment.processor.dto.PaymentProcessorResponse;
import com.springboot.razorpay.vault.dto.request.TokenizeRequest;
import com.springboot.razorpay.vault.dto.response.TokenizeResponse;

import java.util.Map;
import java.util.UUID;

public interface VaultService {
    // Here we can add the method to actually tokenize
    TokenizeResponse tokenize(TokenizeRequest request, UUID merchantId);

    PaymentProcessorResponse charge(UUID paymentId, String token, Money amount, Map<String, Object> methodDetails);
}
