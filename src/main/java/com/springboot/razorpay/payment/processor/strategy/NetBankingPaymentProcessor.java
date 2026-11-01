package com.springboot.razorpay.payment.processor.strategy;

import com.springboot.razorpay.common.util.RandomizerUtil;
import com.springboot.razorpay.payment.processor.PaymentProcessor;
import com.springboot.razorpay.payment.processor.dto.PaymentProcessorRequest;
import com.springboot.razorpay.payment.processor.dto.PaymentProcessorResponse;
import org.springframework.stereotype.Component;

@Component
public class NetBankingPaymentProcessor implements PaymentProcessor {
    @Override
    public PaymentProcessorResponse charge(PaymentProcessorRequest request) {

        final String BANK_CODE_FAIL = "BANK_CODE_FAIL";

        String bankCode = request.methodDetails() != null ?
                request.methodDetails().get("bank").toString() : null;

        // Simulation rule: force failure for a specific test bank code.
        if(BANK_CODE_FAIL.equals(bankCode)){
            return new PaymentProcessorResponse.Failure("BANK_REJECTED",
                    "Bank rejected");
        }

        String processorRef = "NBK_PROCESSOR_" + RandomizerUtil.randomBase64(16);
//        String redirectRef = "http://REDIRECT_BANK.com/" + processorRef;
        return new PaymentProcessorResponse.Pending(processorRef);
    }
}
