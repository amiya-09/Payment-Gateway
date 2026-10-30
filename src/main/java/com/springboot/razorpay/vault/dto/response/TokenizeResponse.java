package com.springboot.razorpay.vault.dto.response;

import com.springboot.razorpay.common.enums.CardBrand;

public record TokenizeResponse (
        // What do we want to return ?
        // We want to return the actual token
        // We want to tokenize our PAN, and we return that token here so that using this token they can again make an
        // API call to initiate the card payment
        String token,
        String lastFour,
        CardBrand brand,
        Integer expiryMonth,
        Integer expiryYear
) {
}
