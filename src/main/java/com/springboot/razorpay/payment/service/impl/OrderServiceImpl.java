package com.springboot.razorpay.payment.service.impl;

import com.springboot.razorpay.common.enums.OrderStatus;
import com.springboot.razorpay.common.exception.DuplicateResourceException;
import com.springboot.razorpay.payment.dto.request.CreateOrderRequest;
import com.springboot.razorpay.payment.dto.response.OrderResponse;
import com.springboot.razorpay.payment.entity.OrderRecord;
import com.springboot.razorpay.payment.repository.OrderRepository;
import com.springboot.razorpay.payment.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    @Value("${order.default.expiry.minutes:15}")
    private int defaultOrderExpiryMinutes;

    private final OrderRepository orderRepository;

    @Override
    public OrderResponse createOrder(UUID merchantId, CreateOrderRequest orderRequest) {
        // We should first check that if the merchant has provided the receipt
        // Then we should check if the receipt exists in the database or not
        // and if it does then we should not create a new order
        if(orderRequest.receipt() != null && orderRepository.existsByMerchantIdAndReceipt(merchantId, orderRequest.receipt())){
            throw new DuplicateResourceException("ORDER_ALREADY_EXISTS", "Order with receipt "
                    + orderRequest.receipt() + " already exists for merchant " + merchantId);
        }
        OrderRecord orderRecord = OrderRecord.builder()
                .receipt(orderRequest.receipt())
                .amount(orderRequest.amount())
                .notes(orderRequest.notes())
                .merchantId(merchantId)
                .orderStatus(OrderStatus.CREATED)
                .expiresAt(orderRequest.expiresAt()!=null ? orderRequest.expiresAt() : LocalDateTime.now().plusMinutes(defaultOrderExpiryMinutes))
                .build();

        orderRecord = orderRepository.save(orderRecord);
        return new OrderResponse(
                orderRecord.getId(),
                orderRecord.getMerchantId(),
                orderRecord.getReceipt(),
                orderRecord.getAmount(),
                orderRecord.getOrderStatus(),
                orderRecord.getAttempts(),
                orderRecord.getNotes(),
                orderRecord.getExpiresAt(),
                null
        );
    }
}
