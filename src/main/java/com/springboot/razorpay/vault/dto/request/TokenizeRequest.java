package com.springboot.razorpay.vault.dto.request;

import com.springboot.razorpay.vault.validation.ExpiryYear;
import jakarta.validation.constraints.*;
import org.hibernate.validator.constraints.LuhnCheck;

import java.util.UUID;

public record TokenizeRequest (
        @NotBlank(message = "PAN is required")
        @LuhnCheck(message = "Invalid Card Number")
        @Pattern(regexp = "^[0-9]{13,19}$", message = "Invalid Card Number")
        String pan,

        @NotBlank(message = "CVV is required")
        @Pattern(regexp = "^[0-9]{3,4}$", message = "Invalid CVV")
        String cvv,

        @NotNull(message = "Expiry month is required")
        @Min(value = 1, message = "Invalid expiry month")
        @Max(value = 12, message = "Invalid expiry month")
        Integer expiryMonth,

        @NotNull(message = "Expiry year is required")
        @ExpiryYear(message = "Expiry year cannot be in the past")
        Integer expiryYear,
        UUID customerId,
        @Size(min = 3,  message = "Card Holder Name should be at least 3 characters long")
        String cardHolderName
) {
}
