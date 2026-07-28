package com.springboot.razorpay.payment.service;

import com.springboot.razorpay.payment.dto.request.CreateOrderRequest;
import com.springboot.razorpay.payment.dto.response.OrderResponse;
import com.springboot.razorpay.payment.dto.response.PaymentResponse;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderResponse createOrder(UUID merchantId, CreateOrderRequest orderRequest);

    OrderResponse getById(UUID merchantId, UUID orderId);

    OrderResponse cancel(UUID merchantId, UUID orderId);

    List<PaymentResponse> listPayments(UUID merchantId, UUID orderId);
}
