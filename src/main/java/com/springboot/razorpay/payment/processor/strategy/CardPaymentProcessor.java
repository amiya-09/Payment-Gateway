package com.springboot.razorpay.payment.processor.strategy;

import com.springboot.razorpay.common.util.RandomizerUtil;
import com.springboot.razorpay.payment.processor.PaymentProcessor;
import com.springboot.razorpay.payment.processor.dto.PaymentProcessorRequest;
import com.springboot.razorpay.payment.processor.dto.PaymentProcessorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class CardPaymentProcessor implements PaymentProcessor {

    public static final String PAN_CARD_DECLINED = "4000000000000002";
    public static final String PAN_CARD_EXPIRED = "4000000000000069";

    @Override
    public PaymentProcessorResponse charge(PaymentProcessorRequest request) {
        String pan = request.pan();
        if(PAN_CARD_DECLINED.equals(pan)) {
            log.warn("Card declined");
            return new PaymentProcessorResponse.Failure("CARD_DECLINED", "Card declined");
        }
        if(PAN_CARD_EXPIRED.equals(pan)) {
            log.warn("Card expired");
            return new PaymentProcessorResponse.Failure("CARD_EXPIRED", "Card expired");
        }
        String processorRef = "CARD_PROCESSOR_" + RandomizerUtil.randomBase64(16);
        return new PaymentProcessorResponse.Pending(processorRef);

    }
}
