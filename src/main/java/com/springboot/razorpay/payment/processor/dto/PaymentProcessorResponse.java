package com.springboot.razorpay.payment.processor.dto;

public sealed interface PaymentProcessorResponse permits
        PaymentProcessorResponse.Pending,
        PaymentProcessorResponse.Success,
        PaymentProcessorResponse.Failure {

    record Pending(String prcessorReference)  implements PaymentProcessorResponse {}
    record Success(String prcessorReference, String bankReference)  implements PaymentProcessorResponse {}
    record Failure(String errorCode, String errorDescription)  implements PaymentProcessorResponse {}
}
