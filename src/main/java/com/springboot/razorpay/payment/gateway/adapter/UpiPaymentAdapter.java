package com.springboot.razorpay.payment.gateway.adapter;

import com.springboot.razorpay.payment.gateway.PaymentAdapter;
import com.springboot.razorpay.payment.gateway.dto.PaymentRequest;
import com.springboot.razorpay.payment.gateway.dto.PaymentResult;

public class UpiPaymentAdapter implements PaymentAdapter {

    @Override
    public PaymentResult initiate(PaymentRequest request) {
        // Implementation for initiating UPI payment
        return null;
    }
}
