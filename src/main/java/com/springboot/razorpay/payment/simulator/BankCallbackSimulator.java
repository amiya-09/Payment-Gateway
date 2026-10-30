package com.springboot.razorpay.payment.simulator;

import com.springboot.razorpay.common.enums.PaymentStatus;
import com.springboot.razorpay.payment.entity.Payment;
import com.springboot.razorpay.payment.repository.PaymentRepository;
import com.springboot.razorpay.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class BankCallbackSimulator {

    private final PaymentRepository paymentRepository;
    private final PaymentService paymentService;
    private final SimulatorConfig simulatorConfig;

    @Scheduled(fixedDelayString = "${payment.simulator.poll-interval-ms:5000}")
    public void processCallbacks() {
        LocalDateTime globalWindow = LocalDateTime.now().minusSeconds(1);
        List<Payment> candidates = paymentRepository.findByStatusAndCreatedAtBefore(PaymentStatus.AUTHORIZING, globalWindow);
        if(candidates.isEmpty()) return;
        for(Payment payment : candidates) {
            simulateCallback(payment);
        }
    }

    private void simulateCallback(Payment payment) {}

}
