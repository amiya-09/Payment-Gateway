package com.springboot.razorpay.payment.simulator;

import com.springboot.razorpay.common.enums.ChaosMode;
import com.springboot.razorpay.common.enums.PaymentMethod;
import com.springboot.razorpay.common.enums.PaymentStatus;
import com.springboot.razorpay.common.util.RandomizerUtil;
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
    // Runs at a fixed delay configured via properties.
    // Default poll interval is 5000 ms (5 seconds).
    public void processCallbacks() {
        LocalDateTime globalWindow = LocalDateTime.now().minusSeconds(1);
        List<Payment> candidates = paymentRepository.findByStatusAndCreatedAtBefore(PaymentStatus.AUTHORIZING, globalWindow);
        log.info("Simulating payments for {} payments", candidates.size());
        if(candidates.isEmpty()) return;
        for(Payment payment : candidates) {
            simulateCallback(payment);
        }
    }

    private void simulateCallback(Payment payment) {
        SimulatorConfig.MethodSimulatorConfig methodConfig = simulatorConfig.configFor(payment.getMethod());
        LocalDateTime dueAt = dueAt(payment, methodConfig); // Time at which this payment should be processed.
        // In other words, when it can move from AUTHORIZING to resolved.

        // If not due yet, skip for now and let the next scheduler cycle handle it.
        if(LocalDateTime.now().isBefore(dueAt)) {
            return;
        }
        ChaosMode chaosMode = simulatorConfig.getChaosMode();
        switch (chaosMode) {
            case SUCCESS -> resolve(payment, true);
            case FAILURE -> resolve(payment, false);
            case TIMEOUT -> {
                // Intentionally do nothing to simulate timeout behavior.
                log.debug("BankCallback simulator: Payment Timed out");
            }
            case NORMAL, SLOW -> resolve(payment, shouldApprove(payment, methodConfig));
        }
    }

    private void resolve(Payment payment, boolean approve) {
        if(approve) {
            String bankRef = "SIM_BANK_REF"+ RandomizerUtil.randomBase64(8);
            paymentService.resolveAuthorization(payment.getId(), true, bankRef, null, null);
        } else {
            paymentService.resolveAuthorization(payment.getId(), false, null, "SIM_BANK_ERROR_CODE", "Simulated Bank Declined");
        }
    }

    private boolean shouldApprove(Payment payment, SimulatorConfig.MethodSimulatorConfig methodConfig) {
        int bucket = Math.abs(payment.getId().hashCode()) % 100; // Deterministic bucket in range [0, 99].
        // This bucket remains stable for the same payment ID.
        return bucket < methodConfig.getSuccessRate();
        // Example: with successRate=90, buckets 0..89 approve and 90..99 reject.
    }

    private LocalDateTime dueAt(Payment payment, SimulatorConfig.MethodSimulatorConfig methodConfig) {
        // Compute when this payment becomes eligible for callback simulation.
        // A fetched payment may still be early, so we compare current time against this value.
        // If not due, we skip it and process in a later scheduler run.
        int range = methodConfig.getMaxDelaySeconds() - methodConfig.getMinDelaySeconds();
        int delaySeconds = methodConfig.getMinDelaySeconds() + Math.abs(payment.getId().hashCode()) % range; // Deterministic per-payment delay.
        // This determines the simulated callback time.
        if(simulatorConfig.getChaosMode() == ChaosMode.SLOW) {
            delaySeconds *= 2;
        }
        return payment.getCreatedAt().plusSeconds(delaySeconds);
    }

}
