package com.springboot.razorpay.vault.dto.response;

import com.springboot.razorpay.common.enums.CardBrand;

public record TokenizeResponse (
        // Return the generated token and card display metadata.
        // Clients use this token in later card-payment API calls.
        String token,
        String lastFour,
        CardBrand brand,
        Integer expiryMonth,
        Integer expiryYear
) {
}
