package com.springboot.razorpay.operations.settlement;

import com.springboot.razorpay.common.entity.Money;
import com.springboot.razorpay.common.util.RandomizerUtil;
import com.springboot.razorpay.operations.settlement.dto.BankTransferResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Slf4j
public class BankTransferProcessorImpl implements BankTransferProcessor {
    @Override
    public BankTransferResult initiate(UUID settlementId, UUID merchantId, Money amount, String bankAccount, String ifsc) {
        // Call the Bank API
        String registrationRef = "TXN_"+ RandomizerUtil.randomBase64(12);
        log.debug("Initiating bank transfer with registration reference: {}, for settlementId: {}", registrationRef,  settlementId);
        return new BankTransferResult(registrationRef);
    }
}
