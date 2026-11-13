package com.springboot.razorpay.operations.settlement;

import com.springboot.razorpay.common.enums.SettlementStatus;
import com.springboot.razorpay.common.util.RandomizerUtil;
import com.springboot.razorpay.operations.entity.Settlement;
import com.springboot.razorpay.operations.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BankSettlementCallbackSimulator {

    private final SettlementRepository settlementRepository;
    private final SettlementTransactionExecutor settlementTransactionExecutor;

    @Scheduled(fixedDelayString = "5000")
    public void processCallbacks(){
        List<Settlement> settlements = settlementRepository.findByStatus(SettlementStatus.TRANSFER_PENDING);
        log.info("Simulating settlement callbacks for {} settlements", settlements.size());
        if(settlements.isEmpty()) return;

        for(Settlement settlement : settlements){
            simulateCallback(settlement);
        }
    }

    private void simulateCallback(Settlement settlement){
        // Simulate a callback from the bank for the settlement
        // For simplicity, we will just mark the settlement as SETTLED
        log.info("Simulating callback for settlementId: {}, merchantId: {}", settlement.getId(), settlement.getMerchantId());
        settlementTransactionExecutor.resolveTransfer(settlement.getId(), null, null);
    }
}
