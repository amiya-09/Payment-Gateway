package com.springboot.razorpay.common.dto;

public record SettlementBankDetails(String accountNumber, String ifscCode, String accountHolderName) {
}
