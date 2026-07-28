package com.springboot.razorpay.payment.service.impl;

import com.springboot.razorpay.common.enums.OrderStatus;
import com.springboot.razorpay.common.exception.BusinessRuleViolationException;
import com.springboot.razorpay.common.exception.DuplicateResourceException;
import com.springboot.razorpay.common.exception.ResourceNotFoundException;
import com.springboot.razorpay.payment.dto.request.CreateOrderRequest;
import com.springboot.razorpay.payment.dto.response.OrderResponse;
import com.springboot.razorpay.payment.dto.response.PaymentResponse;
import com.springboot.razorpay.payment.entity.OrderRecord;
import com.springboot.razorpay.payment.entity.Payment;
import com.springboot.razorpay.payment.mapper.OrderMapper;
import com.springboot.razorpay.payment.mapper.PaymentMapper;
import com.springboot.razorpay.payment.repository.OrderRepository;
import com.springboot.razorpay.payment.repository.PaymentRepository;
import com.springboot.razorpay.payment.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderServiceImpl implements OrderService {

    @Value("${order.default.expiry.minutes:15}")
    private int defaultOrderExpiryMinutes;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OrderMapper orderMapper;

    @Override
    @Transactional
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
//        return new OrderResponse(
//                orderRecord.getId(),
//                orderRecord.getMerchantId(),
//                orderRecord.getReceipt(),
//                orderRecord.getAmount(),
//                orderRecord.getOrderStatus(),
//                orderRecord.getAttempts(),
//                orderRecord.getNotes(),
//                orderRecord.getExpiresAt(),
//                null
//        );
        return orderMapper.toOrderResponse(orderRecord);
    }

    @Override
    public OrderResponse getById(UUID merchantId, UUID orderId) {
        // any merchant should not see other merchant's orders and that is why we have passed merchantId as well
        OrderRecord order =  orderRepository.findByIdAndMerchantId(merchantId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "Order with id " + orderId + " not found for merchant " + merchantId));
        // now we have order, now we need to convert it into Order Response
//        return new OrderResponse(
//                order.getId(),
//                order.getMerchantId(),
//                order.getReceipt(),
//                order.getAmount(),
//                order.getOrderStatus(),
//                order.getAttempts(),
//                order.getNotes(),
//                order.getExpiresAt(),
//                null
//        );
        return orderMapper.toOrderResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancel(UUID merchantId, UUID orderId) {
        OrderRecord order =  orderRepository.findByIdAndMerchantId(merchantId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "Order with id " + orderId + " not found for merchant " + merchantId));
        if(order.getOrderStatus() == OrderStatus.CANCELLED || order.getOrderStatus() == OrderStatus.PAID){
            throw new BusinessRuleViolationException("ORDER_ALREADY_CANCELLED", "Order with id " + orderId + " is already cancelled");
        }
        order.setOrderStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);
        return orderMapper.toOrderResponse(order);
    }

    @Override
    public List<PaymentResponse> listPayments(UUID merchantId, UUID orderId) {
        OrderRecord order =  orderRepository.findByIdAndMerchantId(merchantId, orderId)
                .orElseThrow(() -> new ResourceNotFoundException("ORDER_NOT_FOUND", "Order with id " + orderId + " not found for merchant " + merchantId));
        List<Payment> paymentList = paymentRepository.findByOrder_Id(order);
//        return paymentList.stream()
//                .map(paymentMapper::toPaymentResponse)
//                .collect(Collectors.toList());
        // Map Struct can also convert a list of payments to a list of payment response
        // In the PaymentMapper interface -- List<PaymentResponse> toPaymentResponseList(List<Payment> paymentList);
        return paymentMapper.toPaymentResponseList(paymentList);
    }
}
