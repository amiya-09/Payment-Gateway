package com.springboot.razorpay.payment.processor;

import com.springboot.razorpay.common.enums.PaymentMethod;
import com.springboot.razorpay.payment.processor.dto.PaymentProcessorRequest;
import com.springboot.razorpay.payment.processor.dto.PaymentProcessorResponse;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PaymentProcessorRouter {

    private Map<PaymentMethod, PaymentProcessor> paymentProcessors;

    public PaymentProcessorResponse charge(PaymentProcessorRequest request) {
        PaymentProcessor processor = paymentProcessors.get(request.method());
        if(processor == null) {
            throw new IllegalArgumentException("Payment method not supported: " + request.method());
        }
        return processor.charge(request);
    }
}
