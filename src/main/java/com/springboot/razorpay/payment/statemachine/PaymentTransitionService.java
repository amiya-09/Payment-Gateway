package com.springboot.razorpay.payment.statemachine;

import com.springboot.razorpay.common.enums.PaymentActor;
import com.springboot.razorpay.common.enums.PaymentEvent;
import com.springboot.razorpay.common.enums.PaymentStatus;
import com.springboot.razorpay.payment.entity.Payment;
import com.springboot.razorpay.payment.entity.PaymentTransitionLog;
import com.springboot.razorpay.payment.repository.PaymentTransitionLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PaymentTransitionService {

    private final PaymentTransitionLogRepository paymentTransitionLogRepository;
    private final PaymentStateMachine paymentStateMachine;

    public PaymentStatus apply(Payment payment, PaymentEvent event) {
        PaymentStatus previous = payment.getStatus();
        PaymentStatus next = paymentStateMachine.transition(previous, event);
        PaymentTransitionLog log = PaymentTransitionLog.builder()
                .payment(payment)
                .fromStatus(previous)
                .toStatus(next)
                .event(event)
                .actor(PaymentActor.SYSTEM) // TODO: fetch merchant context to identify actual actor.
                .occurredAt(LocalDateTime.now())
                .build();
        payment.setStatus(next);
        paymentTransitionLogRepository.save(log);
        return next;
    }

}
