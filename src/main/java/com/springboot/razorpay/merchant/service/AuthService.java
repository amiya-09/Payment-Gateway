package com.springboot.razorpay.merchant.service;

import com.springboot.razorpay.merchant.dto.request.LoginRequest;
import com.springboot.razorpay.merchant.dto.request.MerchantSignupRequest;
import com.springboot.razorpay.merchant.dto.response.LoginResponse;
import com.springboot.razorpay.merchant.dto.response.MerchantResponse;
import jakarta.validation.Valid;

public interface AuthService {
    MerchantResponse signup(MerchantSignupRequest request);

    LoginResponse login( LoginRequest request);
}
