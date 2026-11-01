package com.springboot.razorpay.payment.controller;

import com.springboot.razorpay.merchant.security.MerchantContext;
import com.springboot.razorpay.payment.dto.request.CreateOrderRequest;
import com.springboot.razorpay.payment.dto.response.OrderResponse;
import com.springboot.razorpay.payment.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final MerchantContext merchantContext;

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> create(@RequestBody @Valid CreateOrderRequest orderRequest){
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(orderService.createOrder(merchantContext.getMerchantId(), orderRequest));
    }
}