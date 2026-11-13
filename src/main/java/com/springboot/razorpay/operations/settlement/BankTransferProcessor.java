package com.springboot.razorpay.operations.settlement;

import com.springboot.razorpay.common.entity.Money;
import com.springboot.razorpay.operations.settlement.dto.BankTransferResult;

import java.util.UUID;

public interface BankTransferProcessor {
    BankTransferResult initiate(UUID settlementId, UUID merchantId, Money amount, String bankAccount, String ifsc);
}
