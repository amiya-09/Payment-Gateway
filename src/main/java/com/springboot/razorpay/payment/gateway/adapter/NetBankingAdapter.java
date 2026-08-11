package com.springboot.razorpay.payment.gateway.adapter;

import com.springboot.razorpay.payment.gateway.PaymentAdapter;
import com.springboot.razorpay.payment.gateway.dto.PaymentRequest;
import com.springboot.razorpay.payment.gateway.dto.PaymentResult;

public class NetBankingAdapter implements PaymentAdapter {

    @Override
    public PaymentResult initiate(PaymentRequest request) {
        return null;
        // Implementation for initiating net banking payment
    }
}
