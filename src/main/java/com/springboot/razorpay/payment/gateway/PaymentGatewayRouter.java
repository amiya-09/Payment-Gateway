package com.springboot.razorpay.payment.gateway;

import com.springboot.razorpay.common.enums.PaymentMethod;
import com.springboot.razorpay.payment.gateway.dto.PaymentRequest;
import com.springboot.razorpay.payment.gateway.dto.PaymentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentGatewayRouter {

    private final Map<PaymentMethod, PaymentAdapter> paymentAdapters;

    public PaymentResult initiate(PaymentRequest request){
        PaymentAdapter adapter = paymentAdapters.get(request.paymentMethod());
        if(adapter == null){
            throw new IllegalArgumentException("Payment method not supported for: " + request.paymentMethod());
        }
        return adapter.initiate(request);
    }

    public PaymentResult capture(PaymentMethod method, UUID paymentId) {
        PaymentAdapter adapter = paymentAdapters.get(method);
        if(adapter == null){
            throw new IllegalArgumentException("Payment method not supported for: " + method);
        }
        return adapter.capture(paymentId);
    }
}
