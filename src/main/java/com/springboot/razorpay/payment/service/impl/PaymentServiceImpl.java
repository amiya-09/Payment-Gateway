package com.springboot.razorpay.payment.service.impl;

import com.springboot.razorpay.common.enums.OrderStatus;
import com.springboot.razorpay.common.enums.PaymentEvent;
import com.springboot.razorpay.common.enums.PaymentStatus;
import com.springboot.razorpay.common.exception.BusinessRuleViolationException;
import com.springboot.razorpay.common.exception.ResourceNotFoundException;
import com.springboot.razorpay.payment.dto.request.PaymentInitRequest;
import com.springboot.razorpay.payment.dto.response.PaymentResponse;
import com.springboot.razorpay.payment.entity.OrderRecord;
import com.springboot.razorpay.payment.entity.Payment;
import com.springboot.razorpay.payment.gateway.PaymentGatewayRouter;
import com.springboot.razorpay.payment.gateway.dto.PaymentRequest;
import com.springboot.razorpay.payment.gateway.dto.PaymentResult;
import com.springboot.razorpay.payment.mapper.PaymentMapper;
import com.springboot.razorpay.payment.repository.OrderRepository;
import com.springboot.razorpay.payment.repository.PaymentRepository;
import com.springboot.razorpay.payment.service.PaymentService;
import com.springboot.razorpay.payment.statemachine.PaymentTransitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGatewayRouter paymentGatewayRouter;
    private final PaymentMapper paymentMapper;
    private final PaymentTransitionService paymentTransitionService;

    @Override
    @Transactional
    public PaymentResponse initiate(UUID merchantId, PaymentInitRequest request) {
        OrderRecord order = orderRepository.findByIdAndMerchantId(request.orderId(), merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Order" , request.orderId()));
        // We got the orders from here, now we need to check whether these orders
        // are in valid state or not --> ORDER_STATUS
        if(order.getOrderStatus() != OrderStatus.CREATED && order.getOrderStatus() != OrderStatus.ATTEMPTED){
            throw new BusinessRuleViolationException("ORDER_NOT_PAYBLE",
                    "Order is not in valid state for payment initiation. Current status: "+ order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.ATTEMPTED);
        order.setAttempts(order.getAttempts() + 1);

        Payment payment = Payment.builder()
                .order(order)
                .merchantId(merchantId)
                .amount(order.getAmount())
                .status(PaymentStatus.CREATED)
                .method(request.method())
                .methodDetails(request.methodDetails())
                .build();
        payment = paymentRepository.save(payment);

        PaymentRequest paymentRequest = new PaymentRequest(payment.getId(), request.orderId(),
                merchantId, order.getAmount(), request.method(), request.methodDetails());

        PaymentResult result = paymentGatewayRouter.initiate(paymentRequest);
        switch (result) {
            case PaymentResult.Pending pending -> payment.setProcessorReference(pending.registrationRef());
            case PaymentResult.Failure failure -> {
//                payment.setStatus(PaymentStatus.FAILED);
                paymentTransitionService.apply(payment, PaymentEvent.AUTHORIZE_FAIL);
                payment.setErrorCode(failure.errorCode());
                payment.setErrorDescription(failure.errorDescription());
            }
            case PaymentResult.Success success -> {
                log.warn("Invalid State");
                return null;
            }
        }
        payment =  paymentRepository.save(payment);
        orderRepository.save(order);
        // TODO: send an outbox (kafka event)
        return paymentMapper.toPaymentResponse(payment);
    }

    @Override
    public PaymentResponse capture(UUID merchantId, UUID paymentId) {
        Payment payment = paymentRepository.findByIdAndMerchantId(paymentId, merchantId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId));

        if(payment.getStatus() != PaymentStatus.CREATED){
            throw new BusinessRuleViolationException("PAYMENT_NOT_CAPTUREABLE",
                    "Payment is not in valid state for capture. Current status: "+ payment.getStatus());
        }

        paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_REQUEST);

        PaymentResult result = paymentGatewayRouter.capture(payment.getMethod(), paymentId);

        if(result instanceof PaymentResult.Success success) {
            paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_SUCCESS);
            payment.setCapturedAt(LocalDateTime.now());
            log.info("Payment captured , paymentId: " + paymentId);
        }
        else if(result instanceof PaymentResult.Failure failure) {
            paymentTransitionService.apply(payment, PaymentEvent.CAPTURE_FAIL);
            payment.setErrorCode(failure.errorCode());
            payment.setErrorDescription(failure.errorDescription());
            log.warn("Payment capture failed , paymentId: " + paymentId);
        }
        payment =  paymentRepository.save(payment);

        return paymentMapper.toPaymentResponse(payment);
    }

}
