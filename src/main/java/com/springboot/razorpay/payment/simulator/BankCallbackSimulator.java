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
    // This method is scheduled to run at a fixed delay, which is configurable via the application properties.
    // The default delay is set to 5000 milliseconds (5 seconds).
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
        LocalDateTime dueAt = dueAt(payment, methodConfig); // This gives us the time when this payment should be processed
        // meaning when it should go from authorizing state to the authorized state.

        // If this payment has come too early, we will not process it and wait for the next scheduled cycle to process it.
        if(LocalDateTime.now().isBefore(dueAt)) {
            return;
        }
        ChaosMode chaosMode = simulatorConfig.getChaosMode();
        switch (chaosMode) {
            case SUCCESS -> resolve(payment, true);
            case FAILURE -> resolve(payment, false);
            case TIMEOUT -> {
                // Do nothing, we are simulating a timeout scenario here
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
        int bucket = Math.abs(payment.getId().hashCode()) % 100; // Bucket is going to give me a number ranging between 0 and 99
        // But this number would always remain the same for a particular payment object
        return bucket < methodConfig.getSuccessRate();
        // Suppose the success rate for this type of payment was 90%. In that case methodConfig.getSuccessRate() would return 90.
        // So if the bucket is less than 90, we will approve it. Otherwise, we will reject it.
    }

    private LocalDateTime dueAt(Payment payment, SimulatorConfig.MethodSimulatorConfig methodConfig) {
        // This dueAt would basically give us the time when this payment should be processed
        // When we are getting the payment object from the window, the payment itself is not due to be processed
        // Maybe we will process it in the next go, in that case we are just going to check first, if it is due then we are going to process it
        // Otherwise we are going to ignore it, and it can be processed in the next scheduled cycle
        int range = methodConfig.getMaxDelaySeconds() - methodConfig.getMinDelaySeconds();
        int delaySeconds = methodConfig.getMinDelaySeconds() + Math.abs(payment.getId().hashCode()) % range; // What this does is :
        // It's going to give me when this payment should be simulated
        if(simulatorConfig.getChaosMode() == ChaosMode.SLOW) {
            delaySeconds *= 2;
        }
        return payment.getCreatedAt().plusSeconds(delaySeconds);
    }

}
