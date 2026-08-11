package com.springboot.razorpay.payment.gateway;

import com.springboot.razorpay.common.enums.PaymentMethod;
import com.springboot.razorpay.payment.gateway.dto.PaymentRequest;
import com.springboot.razorpay.payment.gateway.dto.PaymentResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class PaymentGatewayRouter {

    private final Map<PaymentMethod, PaymentAdapter> paymentAdapters;
    public PaymentResult initiate(PaymentRequest request){
        PaymentAdapter adapter = paymentAdapters.get(request.getPaymentMethod());
        if(adapter == null){
            throw new IllegalArgumentException("Payment method not supported for: " + request.getPaymentMethod());
        }
        return adapter.initiate(request);
    }
}
