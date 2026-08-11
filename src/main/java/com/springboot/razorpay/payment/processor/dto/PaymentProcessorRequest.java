package com.springboot.razorpay.payment.processor.dto;

import com.springboot.razorpay.common.entity.Money;
import com.springboot.razorpay.common.enums.PaymentMethod;

import java.util.Map;

public record PaymentProcessorRequest(
        PaymentMethod method,
        Money amount,
        Map<String, Object> methodDetails
) {
}
