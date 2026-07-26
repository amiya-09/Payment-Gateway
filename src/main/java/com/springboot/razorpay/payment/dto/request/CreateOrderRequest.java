package com.springboot.razorpay.payment.dto.request;

import com.springboot.razorpay.common.entity.Money;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.Map;

public record CreateOrderRequest(
        @NotNull(message = "Amount is required")
        Money amount,
        @Size(max = 100, message = "Receipt length must be less than or equal to 100 characters")
        String receipt, // order-id (known to merchant)
        Map<String, Object> notes,
        LocalDateTime expiresAt

) {
}
